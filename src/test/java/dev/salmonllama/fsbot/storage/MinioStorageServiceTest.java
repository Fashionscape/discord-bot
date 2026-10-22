/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.storage;

import dev.salmonllama.fsbot.config.BotProperties;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<byte[]> httpResponse;

    private BotProperties properties;
    private MinioStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new BotProperties(
                new BotProperties.DiscordProperties("test-token", 123456L, "guild-1"),
                new BotProperties.MinioProperties(
                        "http://localhost:9000",
                        "minioadmin",
                        "minioadmin",
                        "test-bucket",
                        "https://media.fashionscape.org"
                )
        );

        storageService = new MinioStorageService(minioClient, properties, httpClient);
    }

    @Test
    void ensureBucketExistsCreatesBucketWhenMissing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        storageService.ensureBucketExists();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void ensureBucketExistsDoesNotCreateBucketWhenPresent() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storageService.ensureBucketExists();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void uploadBytesSuccessfullyPutsObject() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        byte[] payload = "test-image-content".getBytes(StandardCharsets.UTF_8);

        UploadResult result = storageService.upload("outfits/outfit-1.png", payload, "image/png");

        assertNotNull(result);
        assertEquals("outfits/outfit-1.png", result.objectName());
        assertEquals("test-bucket", result.bucket());
        assertEquals(payload.length, result.size());
        assertEquals("image/png", result.contentType());
        assertEquals("https://media.fashionscape.org/outfits/outfit-1.png", result.publicUrl());

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs putArgs = captor.getValue();
        assertEquals("test-bucket", putArgs.bucket());
        assertEquals("outfits/outfit-1.png", putArgs.object());
        assertEquals("image/png", putArgs.contentType());
    }

    @Test
    void uploadOutfitNamesObjectCorrectly() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        byte[] payload = "image-bytes".getBytes(StandardCharsets.UTF_8);

        UploadResult result = storageService.uploadOutfit("abc-123", payload, "image/jpeg");

        assertEquals("outfits/abc-123.jpg", result.objectName());
        assertEquals("https://media.fashionscape.org/outfits/abc-123.jpg", result.publicUrl());
    }

    @Test
    void uploadOutfitFromUrlDownloadsAndUploads() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

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
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getObjectReturnsStream() throws Exception {
        GetObjectResponse mockStream = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockStream);

        InputStream result = storageService.getObject("outfits/test.png");
        assertSame(mockStream, result);
    }

    @Test
    void objectExistsReturnsTrueWhenPresent() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

        assertTrue(storageService.objectExists("outfits/present.png"));
    }

    @Test
    void objectExistsReturnsFalseWhenNoSuchKey() throws Exception {
        ErrorResponse err = mock(ErrorResponse.class);
        when(err.code()).thenReturn("NoSuchKey");
        ErrorResponseException exception = mock(ErrorResponseException.class);
        when(exception.errorResponse()).thenReturn(err);

        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(exception);

        assertFalse(storageService.objectExists("outfits/missing.png"));
    }

    @Test
    void deleteObjectCallsRemoveObject() throws Exception {
        storageService.deleteObject("outfits/deleted.png");
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
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
}
