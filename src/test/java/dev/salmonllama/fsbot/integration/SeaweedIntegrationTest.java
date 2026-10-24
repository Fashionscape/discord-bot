package dev.salmonllama.fsbot.integration;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.storage.SeaweedStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class SeaweedIntegrationTest {

    @Container
    static SeaweedContainer seaweed = new SeaweedContainer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("fsbot.seaweed.endpoint", seaweed::getS3URL);
        registry.add("fsbot.seaweed.access-key", seaweed::getAccessKey);
        registry.add("fsbot.seaweed.secret-key", seaweed::getSecretKey);
        registry.add("fsbot.seaweed.bucket", () -> "fsbot-integration-test");
        registry.add("fsbot.seaweed.public-url", () -> "https://media.fashionscape.org");
        registry.add("fsbot.discord.token", () -> "");
    }

    @Autowired
    private SeaweedStorageService storageService;

    @Autowired
    private BotProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        storageService.prepareBucket();
    }

    private int anonymousGetStatus(String objectName) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(seaweed.getS3URL() + "/fsbot-integration-test/" + objectName))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    @DisplayName("Only outfits/ is publicly readable; quarantined images are not")
    void testQuarantineRemovesPublicAccess() throws Exception {
        byte[] image = new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47 };
        storageService.uploadOutfit("outfit-q-1", image, "image/png");
        assertEquals(200, anonymousGetStatus("outfits/outfit-q-1.png"));

        String quarantined = storageService.quarantine("outfits/outfit-q-1.png");
        assertEquals("deleted/outfits/outfit-q-1.png", quarantined);
        assertFalse(storageService.objectExists("outfits/outfit-q-1.png"));
        assertArrayEquals(image, storageService.getObjectBytes(quarantined));
        assertEquals(403, anonymousGetStatus(quarantined));

        // Repeating the move is harmless
        storageService.quarantine("outfits/outfit-q-1.png");
        assertTrue(storageService.objectExists(quarantined));

        storageService.release("outfits/outfit-q-1.png");
        assertEquals(200, anonymousGetStatus("outfits/outfit-q-1.png"));
        assertFalse(storageService.objectExists(quarantined));

        storageService.deleteObject("outfits/outfit-q-1.png");
    }

    @Test
    @DisplayName("Verify bucket initialization, upload, retrieve, and delete against real SeaweedFS container")
    void testStorageLifecycle() throws Exception {
        byte[] payload = "Hello SeaweedFS Integration Test!".getBytes(StandardCharsets.UTF_8);
        String objectName = "test/integration-file.txt";

        // 1. Upload object
        UploadResult result = storageService.upload(objectName, payload, "text/plain");
        assertNotNull(result);
        assertEquals(objectName, result.objectName());
        assertEquals("fsbot-integration-test", result.bucket());
        assertEquals(payload.length, result.size());
        assertEquals("https://media.fashionscape.org/" + objectName, result.publicUrl());

        // 2. Check object existence
        assertTrue(storageService.objectExists(objectName));
        assertFalse(storageService.objectExists("nonexistent-file.png"));

        // 3. Read object bytes back and verify content
        byte[] retrievedBytes = storageService.getObjectBytes(objectName);
        assertArrayEquals(payload, retrievedBytes);

        // 4. Stream object and verify content
        try (InputStream stream = storageService.getObject(objectName)) {
            byte[] streamedBytes = stream.readAllBytes();
            assertArrayEquals(payload, streamedBytes);
        }

        // 5. Delete object
        storageService.deleteObject(objectName);
        assertFalse(storageService.objectExists(objectName));
    }

    @Test
    @DisplayName("Verify outfit upload naming convention and public URL resolution")
    void testUploadOutfit() throws Exception {
        byte[] fakePng = new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        String outfitId = "outfit-tc-99";

        UploadResult result = storageService.uploadOutfit(outfitId, fakePng, "image/png");
        assertNotNull(result);
        assertEquals("outfits/outfit-tc-99.png", result.objectName());
        assertEquals("https://media.fashionscape.org/outfits/outfit-tc-99.png", result.publicUrl());

        assertTrue(storageService.objectExists("outfits/outfit-tc-99.png"));

        // Extract key from public URL
        var key = storageService.extractObjectKey(result.publicUrl());
        assertTrue(key.isPresent());
        assertEquals("outfits/outfit-tc-99.png", key.get());

        // Clean up
        storageService.deleteObject("outfits/outfit-tc-99.png");
    }
}
