package dev.salmonllama.fsbot.storage;

import dev.salmonllama.fsbot.config.BotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeaweedStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<byte[]> httpResponse;

    private BotProperties properties;
    private SeaweedStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new BotProperties(
                new BotProperties.DiscordProperties("test-token", 123456L, "guild-1", ""),
                new BotProperties.SeaweedProperties(
                        "http://localhost:8333",
                        "seaweedadmin",
                        "seaweedadmin",
                        "test-bucket",
                        "https://media.fashionscape.org"
                )
        );

        storageService = new SeaweedStorageService(s3Client, properties, httpClient);
    }

    @Test
    void ensureBucketExistsCreatesBucketWhenMissing() throws Exception {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().build());

        storageService.ensureBucketExists();

        ArgumentCaptor<CreateBucketRequest> captor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
    }

    @Test
    void ensureBucketExistsDoesNotCreateBucketWhenPresent() throws Exception {
        storageService.ensureBucketExists();

        verify(s3Client).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void uploadBytesSuccessfullyPutsObject() throws Exception {
        byte[] payload = "test-image-content".getBytes(StandardCharsets.UTF_8);

        UploadResult result = storageService.upload("outfits/outfit-1.png", payload, "image/png");

        assertNotNull(result);
        assertEquals("outfits/outfit-1.png", result.objectName());
        assertEquals("test-bucket", result.bucket());
        assertEquals(payload.length, result.size());
        assertEquals("image/png", result.contentType());
        assertEquals("https://media.fashionscape.org/outfits/outfit-1.png", result.publicUrl());

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(request.capture(), body.capture());
        assertEquals("test-bucket", request.getValue().bucket());
        assertEquals("outfits/outfit-1.png", request.getValue().key());
        assertEquals("image/png", request.getValue().contentType());
        assertEquals(Optional.of((long) payload.length), body.getValue().optionalContentLength());
    }

    @Test
    void uploadStreamOfUnknownLengthIsBufferedFirst() throws Exception {
        byte[] payload = new byte[]{9, 8, 7};

        UploadResult result = storageService.upload("outfits/s.png", new java.io.ByteArrayInputStream(payload), -1, null);

        assertEquals(3, result.size());
        assertEquals("application/octet-stream", result.contentType());
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(any(PutObjectRequest.class), body.capture());
        assertEquals(Optional.of(3L), body.getValue().optionalContentLength());
    }

    @Test
    void uploadOutfitNamesObjectCorrectly() throws Exception {
        byte[] payload = "image-bytes".getBytes(StandardCharsets.UTF_8);

        UploadResult result = storageService.uploadOutfit("abc-123", payload, "image/jpeg");

        assertEquals("outfits/abc-123.jpg", result.objectName());
        assertEquals("https://media.fashionscape.org/outfits/abc-123.jpg", result.publicUrl());
    }

    @Test
    void uploadOutfitFromUrlDownloadsAndUploads() throws Exception {
        byte[] fakeImage = new byte[]{1, 2, 3, 4};
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(fakeImage);
        java.net.http.HttpHeaders headers = java.net.http.HttpHeaders.of(
                java.util.Map.of("Content-Type", java.util.List.of("image/png")),
                (k, v) -> true
        );
        when(httpResponse.headers()).thenReturn(headers);

        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        UploadResult result = storageService.uploadOutfitFromUrl("test-outfit", "https://i.imgur.com/example.png");

        assertNotNull(result);
        assertEquals("outfits/test-outfit.png", result.objectName());
        assertEquals("https://media.fashionscape.org/outfits/test-outfit.png", result.publicUrl());
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getObjectReturnsStream() throws Exception {
        ResponseInputStream<GetObjectResponse> mockStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);

        InputStream result = storageService.getObject("outfits/test.png");

        assertSame(mockStream, result);
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
        assertEquals("outfits/test.png", captor.getValue().key());
    }

    @Test
    void objectExistsReturnsTrueWhenPresent() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertTrue(storageService.objectExists("outfits/present.png"));
    }

    @Test
    void objectExistsReturnsFalseWhenNoSuchKey() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertFalse(storageService.objectExists("outfits/missing.png"));
    }

    @Test
    void objectExistsReturnsFalseOnPlain404() {
        // HEAD responses have no body, so some servers' 404s arrive as a plain S3Exception
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        assertFalse(storageService.objectExists("outfits/missing.png"));
    }

    @Test
    void deleteObjectDeletesKey() throws Exception {
        storageService.deleteObject("outfits/deleted.png");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        assertEquals("outfits/deleted.png", captor.getValue().key());
    }

    @Test
    void getPublicUrlHandlesLeadingAndTrailingSlashes() {
        assertEquals("https://media.fashionscape.org/outfits/pic.png", storageService.getPublicUrl("outfits/pic.png"));
        assertEquals("https://media.fashionscape.org/outfits/pic.png", storageService.getPublicUrl("/outfits/pic.png"));
    }

    @Test
    void extractObjectKeyExtractsKeyFromValidUrl() {
        Optional<String> key = storageService.extractObjectKey("https://media.fashionscape.org/outfits/sample.png");
        assertTrue(key.isPresent());
        assertEquals("outfits/sample.png", key.get());

        Optional<String> invalid = storageService.extractObjectKey("https://otherdomain.com/outfits/sample.png");
        assertTrue(invalid.isEmpty());
    }

    @Test
    void resolveExtensionMapsMimeTypesAndUrlsCorrectly() {
        assertEquals("png", storageService.resolveExtension("image/png", null));
        assertEquals("jpg", storageService.resolveExtension("image/jpeg", null));
        assertEquals("jpg", storageService.resolveExtension("image/jpg", null));
        assertEquals("gif", storageService.resolveExtension("image/gif", null));
        assertEquals("webp", storageService.resolveExtension("image/webp", null));

        assertEquals("png", storageService.resolveExtension(null, "https://i.imgur.com/test.png"));
        assertEquals("jpg", storageService.resolveExtension(null, "https://i.imgur.com/test.jpeg"));
        assertEquals("gif", storageService.resolveExtension(null, "https://i.imgur.com/test.gif"));
        assertEquals("webp", storageService.resolveExtension(null, "https://i.imgur.com/test.webp"));
        assertEquals("png", storageService.resolveExtension(null, null));
    }

    @Test
    void prepareBucketLimitsPublicReadToOutfits() throws Exception {
        storageService.prepareBucket();

        ArgumentCaptor<PutBucketPolicyRequest> captor = ArgumentCaptor.forClass(PutBucketPolicyRequest.class);
        verify(s3Client).putBucketPolicy(captor.capture());
        assertEquals("test-bucket", captor.getValue().bucket());
        String policy = captor.getValue().policy();
        assertTrue(policy.contains("\"arn:aws:s3:::test-bucket/outfits/*\""), policy);
        assertTrue(policy.contains("s3:GetObject"));
        assertFalse(policy.contains("deleted/"));
    }

    @Test
    void initializeBucketDoesNotThrowWhenSeaweedIsDown() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(SdkClientException.create("connection refused"));

        assertDoesNotThrow(() -> storageService.initializeBucket());
    }

    @Test
    void uploadPreparesBucketOnlyOnce() throws Exception {
        storageService.upload("outfits/a.png", new byte[]{1}, "image/png");
        storageService.upload("outfits/b.png", new byte[]{2}, "image/png");

        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, times(1)).putBucketPolicy(any(PutBucketPolicyRequest.class));
        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void quarantineCopiesToDeletedPrefixThenRemovesOriginal() throws Exception {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertEquals("deleted/outfits/x.png", storageService.quarantine("outfits/x.png"));

        ArgumentCaptor<CopyObjectRequest> copy = ArgumentCaptor.forClass(CopyObjectRequest.class);
        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        var order = inOrder(s3Client);
        order.verify(s3Client).copyObject(copy.capture());
        order.verify(s3Client).deleteObject(delete.capture());
        assertEquals("test-bucket", copy.getValue().sourceBucket());
        assertEquals("outfits/x.png", copy.getValue().sourceKey());
        assertEquals("test-bucket", copy.getValue().destinationBucket());
        assertEquals("deleted/outfits/x.png", copy.getValue().destinationKey());
        assertEquals("outfits/x.png", delete.getValue().key());
    }

    @Test
    void quarantineIsSafeToRepeat() throws Exception {
        when(s3Client.headObject(argThat((HeadObjectRequest r) -> r != null && r.key().equals("outfits/x.png"))))
                .thenThrow(NoSuchKeyException.builder().build());
        when(s3Client.headObject(argThat((HeadObjectRequest r) -> r != null && r.key().equals("deleted/outfits/x.png"))))
                .thenReturn(HeadObjectResponse.builder().build());

        storageService.quarantine("outfits/x.png");

        verify(s3Client, never()).copyObject(any(CopyObjectRequest.class));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void releaseMovesImageBack() throws Exception {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        storageService.release("outfits/x.png");

        ArgumentCaptor<CopyObjectRequest> copy = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(copy.capture());
        assertEquals("deleted/outfits/x.png", copy.getValue().sourceKey());
        assertEquals("outfits/x.png", copy.getValue().destinationKey());
    }

    @Test
    void quarantineRejectsKeysOutsideOutfits() {
        assertThrows(IllegalArgumentException.class, () -> storageService.quarantine("deleted/outfits/x.png"));
        assertThrows(IllegalArgumentException.class, () -> storageService.release("other/x.png"));
    }
}
