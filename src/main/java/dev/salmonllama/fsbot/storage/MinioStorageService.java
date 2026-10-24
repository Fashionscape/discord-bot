package dev.salmonllama.fsbot.storage;

import dev.salmonllama.fsbot.config.BotProperties;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Object storage for outfit images. The bot runs SeaweedFS and talks to its S3 gateway through the
 * MinIO Java client, which works with any S3-compatible server (see docs/adr/0001-seaweedfs-object-storage.md).
 */
@Service
public class MinioStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioStorageService.class);

    /** Objects under this prefix are publicly readable through the bucket policy. */
    public static final String PUBLIC_PREFIX = "outfits/";
    /** Images of deleted outfits are kept under this prefix, which the bucket policy doesn't expose. */
    public static final String QUARANTINE_PREFIX = "deleted/";

    private final MinioClient minioClient;
    private final BotProperties properties;
    private final HttpClient httpClient;
    private volatile boolean bucketReady = false;

    @org.springframework.beans.factory.annotation.Autowired
    public MinioStorageService(MinioClient minioClient, BotProperties properties) {
        this(minioClient, properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public MinioStorageService(MinioClient minioClient, BotProperties properties, HttpClient httpClient) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.httpClient = httpClient;
    }

    /**
     * Prepares the bucket at startup. A failure (SeaweedFS not reachable yet) is logged and
     * retried on the first upload, so it never stops the bot from starting.
     */
    @PostConstruct
    public void initializeBucket() {
        try {
            prepareBucket();
        } catch (Exception e) {
            logger.warn("Could not prepare SeaweedFS bucket '{}' at startup; will retry on first upload: {}",
                    properties.seaweed().bucket(), e.getMessage());
        }
    }

    /**
     * Creates the bucket if needed and sets its policy so that only {@value #PUBLIC_PREFIX} is
     * publicly readable. This replaces any existing bucket policy.
     */
    public synchronized void prepareBucket() throws Exception {
        ensureBucketExists();
        applyPublicReadPolicy();
        bucketReady = true;
    }

    void applyPublicReadPolicy() throws Exception {
        String bucket = properties.seaweed().bucket();
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .config(publicReadPolicy(bucket))
                .build());
        logger.info("SeaweedFS bucket '{}' policy set: public read on '{}*' only", bucket, PUBLIC_PREFIX);
    }

    static String publicReadPolicy(String bucket) {
        return """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
                "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/%s*"]}]}\
                """.formatted(bucket, PUBLIC_PREFIX);
    }

    private void ensureReady() throws Exception {
        if (!bucketReady) {
            prepareBucket();
        }
    }

    /**
     * Checks if the configured bucket exists, and creates it if it does not.
     */
    public void ensureBucketExists() throws Exception {
        String bucket = properties.seaweed().bucket();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            logger.info("SeaweedFS bucket '{}' created successfully", bucket);
        }
    }

    /**
     * Upload an arbitrary input stream to SeaweedFS.
     */
    public UploadResult upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        ensureReady();

        String bucket = properties.seaweed().bucket();
        long partSize = size >= 0 ? -1 : 10 * 1024 * 1024; // 10MB chunk size for streams of unknown length
        String resolvedContentType = (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(inputStream, size, partSize)
                        .contentType(resolvedContentType)
                        .build()
        );

        String publicUrl = getPublicUrl(objectName);
        logger.debug("Uploaded object '{}' ({} bytes) to bucket '{}', public URL: {}", objectName, size, bucket, publicUrl);
        return new UploadResult(objectName, publicUrl, bucket, size, resolvedContentType);
    }

    /**
     * Upload byte array data to SeaweedFS.
     */
    public UploadResult upload(String objectName, byte[] data, String contentType) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            return upload(objectName, bais, data.length, contentType);
        }
    }

    /**
     * Upload an outfit image byte array, naming it under 'outfits/{outfitId}.{ext}'.
     */
    public UploadResult uploadOutfit(String outfitId, byte[] data, String contentType) throws Exception {
        String ext = resolveExtension(contentType, null);
        String objectName = buildOutfitObjectName(outfitId, ext);
        return upload(objectName, data, contentType);
    }

    /**
     * Upload an outfit image stream, naming it under 'outfits/{outfitId}.{ext}'.
     */
    public UploadResult uploadOutfit(String outfitId, InputStream inputStream, long size, String contentType) throws Exception {
        String ext = resolveExtension(contentType, null);
        String objectName = buildOutfitObjectName(outfitId, ext);
        return upload(objectName, inputStream, size, contentType);
    }

    /**
     * Ingests an image directly from a source URL (e.g. Discord CDN or Imgur) and uploads it to SeaweedFS.
     */
    public UploadResult uploadOutfitFromUrl(String outfitId, String sourceUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sourceUrl))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "FashionScape-Bot/3.0")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            throw new IOException("Failed to fetch image from URL: " + sourceUrl + " (HTTP " + status + ")");
        }

        byte[] imageBytes = response.body();
        String contentType = response.headers().firstValue("Content-Type").orElse("image/png");
        String ext = resolveExtension(contentType, sourceUrl);
        String objectName = buildOutfitObjectName(outfitId, ext);

        return upload(objectName, imageBytes, contentType);
    }

    /**
     * Streams an object from SeaweedFS. Caller is responsible for closing the returned InputStream.
     */
    public InputStream getObject(String objectName) throws Exception {
        String bucket = properties.seaweed().bucket();
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Reads all bytes of an object from SeaweedFS.
     */
    public byte[] getObjectBytes(String objectName) throws Exception {
        try (InputStream stream = getObject(objectName)) {
            return stream.readAllBytes();
        }
    }

    /**
     * Deletes an object from SeaweedFS.
     */
    public void deleteObject(String objectName) throws Exception {
        String bucket = properties.seaweed().bucket();
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
        logger.info("Deleted object '{}' from bucket '{}'", objectName, bucket);
    }

    /**
     * Moves a public outfit image out of public read, to {@value #QUARANTINE_PREFIX} + its key.
     * Safe to repeat: if the image was already moved, nothing happens.
     *
     * @return the quarantined key
     */
    public String quarantine(String publicKey) throws Exception {
        if (!publicKey.startsWith(PUBLIC_PREFIX)) {
            throw new IllegalArgumentException("Only objects under " + PUBLIC_PREFIX + " can be quarantined: " + publicKey);
        }
        String quarantinedKey = QUARANTINE_PREFIX + publicKey;
        move(publicKey, quarantinedKey);
        return quarantinedKey;
    }

    /**
     * Moves a quarantined image back to its public key. Safe to repeat.
     */
    public void release(String publicKey) throws Exception {
        if (!publicKey.startsWith(PUBLIC_PREFIX)) {
            throw new IllegalArgumentException("Only objects under " + PUBLIC_PREFIX + " can be released: " + publicKey);
        }
        move(QUARANTINE_PREFIX + publicKey, publicKey);
    }

    private void move(String from, String to) throws Exception {
        if (!objectExists(from) && objectExists(to)) {
            logger.debug("Object '{}' was already moved to '{}'", from, to);
            return;
        }
        String bucket = properties.seaweed().bucket();
        minioClient.copyObject(CopyObjectArgs.builder()
                .bucket(bucket)
                .object(to)
                .source(CopySource.builder().bucket(bucket).object(from).build())
                .build());
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(from).build());
        logger.info("Moved object '{}' to '{}' in bucket '{}'", from, to, bucket);
    }

    /**
     * Checks if an object exists in SeaweedFS.
     */
    public boolean objectExists(String objectName) {
        String bucket = properties.seaweed().bucket();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            logger.warn("Error checking existence of object '{}' in bucket '{}': {}", objectName, bucket, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("Unexpected error checking object '{}': {}", objectName, e.getMessage());
            return false;
        }
    }

    /**
     * Resolves the public CDN/reverse-proxy URL for a given SeaweedFS object key.
     */
    public String getPublicUrl(String objectName) {
        String baseUrl = properties.seaweed().publicUrl().replaceAll("/+$", "");
        String cleanName = objectName.startsWith("/") ? objectName.substring(1) : objectName;
        return baseUrl + "/" + cleanName;
    }

    /**
     * Extracts the object key from a public URL if it matches the configured public URL prefix.
     */
    public Optional<String> extractObjectKey(String publicUrl) {
        if (publicUrl == null) {
            return Optional.empty();
        }
        String baseUrl = properties.seaweed().publicUrl().replaceAll("/+$", "");
        if (publicUrl.startsWith(baseUrl + "/")) {
            return Optional.of(publicUrl.substring(baseUrl.length() + 1));
        }
        return Optional.empty();
    }

    public String buildOutfitObjectName(String outfitId, String extension) {
        String cleanExt = extension.startsWith(".") ? extension.substring(1) : extension;
        return "outfits/" + outfitId + "." + cleanExt;
    }

    public String resolveExtension(String contentType, String fallbackUrl) {
        if (contentType != null) {
            String cleanType = contentType.split(";")[0].trim().toLowerCase();
            switch (cleanType) {
                case "image/png": return "png";
                case "image/jpeg":
                case "image/jpg": return "jpg";
                case "image/gif": return "gif";
                case "image/webp": return "webp";
                default: break;
            }
        }
        if (fallbackUrl != null) {
            try {
                String path = URI.create(fallbackUrl).getPath();
                if (path != null && path.contains(".")) {
                    String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
                    if (List.of("png", "jpg", "jpeg", "gif", "webp").contains(ext)) {
                        return ext.equals("jpeg") ? "jpg" : ext;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "png";
    }

    public BotProperties getProperties() {
        return properties;
    }
}
