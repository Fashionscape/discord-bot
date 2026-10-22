/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.storage;

import dev.salmonllama.fsbot.config.BotProperties;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
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

@Service
public class MinioStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final BotProperties properties;
    private final HttpClient httpClient;

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
     * Checks if the configured bucket exists, and creates it if it does not.
     */
    public void ensureBucketExists() throws Exception {
        String bucket = properties.minio().bucket();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            logger.info("MinIO bucket '{}' created successfully", bucket);
        }
    }

    /**
     * Upload an arbitrary input stream to MinIO.
     */
    public UploadResult upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        ensureBucketExists();

        String bucket = properties.minio().bucket();
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
     * Upload byte array data to MinIO.
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
     * Ingests an image directly from a source URL (e.g. Discord CDN or Imgur) and uploads it to MinIO.
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
     * Streams an object from MinIO. Caller is responsible for closing the returned InputStream.
     */
    public InputStream getObject(String objectName) throws Exception {
        String bucket = properties.minio().bucket();
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Reads all bytes of an object from MinIO.
     */
    public byte[] getObjectBytes(String objectName) throws Exception {
        try (InputStream stream = getObject(objectName)) {
            return stream.readAllBytes();
        }
    }

    /**
     * Deletes an object from MinIO.
     */
    public void deleteObject(String objectName) throws Exception {
        String bucket = properties.minio().bucket();
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
        logger.info("Deleted object '{}' from bucket '{}'", objectName, bucket);
    }

    /**
     * Checks if an object exists in MinIO.
     */
    public boolean objectExists(String objectName) {
        String bucket = properties.minio().bucket();
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
     * Resolves the public CDN/reverse-proxy URL for a given MinIO object key.
     */
    public String getPublicUrl(String objectName) {
        String baseUrl = properties.minio().publicUrl().replaceAll("/+$", "");
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
        String baseUrl = properties.minio().publicUrl().replaceAll("/+$", "");
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
