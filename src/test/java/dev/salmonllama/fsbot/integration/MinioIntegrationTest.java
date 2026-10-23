package dev.salmonllama.fsbot.integration;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.storage.MinioStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class MinioIntegrationTest {

    @Container
    static MinIOContainer minio = MinioTestContainer.create();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("fsbot.minio.endpoint", minio::getS3URL);
        registry.add("fsbot.minio.access-key", minio::getUserName);
        registry.add("fsbot.minio.secret-key", minio::getPassword);
        registry.add("fsbot.minio.bucket", () -> "fsbot-integration-test");
        registry.add("fsbot.minio.public-url", () -> "https://media.fashionscape.org");
        registry.add("fsbot.discord.token", () -> "");
    }

    @Autowired
    private MinioStorageService minioStorageService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private BotProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        minioStorageService.ensureBucketExists();
    }

    @Test
    @DisplayName("Verify MinIO bucket initialization, upload, retrieve, and delete against real MinIO container")
    void testMinioLifecycle() throws Exception {
        byte[] payload = "Hello MinIO Integration Test!".getBytes(StandardCharsets.UTF_8);
        String objectName = "test/integration-file.txt";

        // 1. Upload object
        UploadResult result = minioStorageService.upload(objectName, payload, "text/plain");
        assertNotNull(result);
        assertEquals(objectName, result.objectName());
        assertEquals("fsbot-integration-test", result.bucket());
        assertEquals(payload.length, result.size());
        assertEquals("https://media.fashionscape.org/" + objectName, result.publicUrl());

        // 2. Check object existence
        assertTrue(minioStorageService.objectExists(objectName));
        assertFalse(minioStorageService.objectExists("nonexistent-file.png"));

        // 3. Read object bytes back and verify content
        byte[] retrievedBytes = minioStorageService.getObjectBytes(objectName);
        assertArrayEquals(payload, retrievedBytes);

        // 4. Stream object and verify content
        try (InputStream stream = minioStorageService.getObject(objectName)) {
            byte[] streamedBytes = stream.readAllBytes();
            assertArrayEquals(payload, streamedBytes);
        }

        // 5. Delete object
        minioStorageService.deleteObject(objectName);
        assertFalse(minioStorageService.objectExists(objectName));
    }

    @Test
    @DisplayName("Verify outfit upload naming convention and public URL resolution")
    void testUploadOutfit() throws Exception {
        byte[] fakePng = new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        String outfitId = "outfit-tc-99";

        UploadResult result = minioStorageService.uploadOutfit(outfitId, fakePng, "image/png");
        assertNotNull(result);
        assertEquals("outfits/outfit-tc-99.png", result.objectName());
        assertEquals("https://media.fashionscape.org/outfits/outfit-tc-99.png", result.publicUrl());

        assertTrue(minioStorageService.objectExists("outfits/outfit-tc-99.png"));

        // Extract key from public URL
        var key = minioStorageService.extractObjectKey(result.publicUrl());
        assertTrue(key.isPresent());
        assertEquals("outfits/outfit-tc-99.png", key.get());

        // Clean up
        minioStorageService.deleteObject("outfits/outfit-tc-99.png");
    }
}
