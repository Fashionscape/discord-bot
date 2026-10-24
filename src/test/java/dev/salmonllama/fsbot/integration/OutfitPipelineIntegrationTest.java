package dev.salmonllama.fsbot.integration;

import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.GalleryChannelRepository;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.services.BlocklistService;
import dev.salmonllama.fsbot.services.OutfitService;
import dev.salmonllama.fsbot.storage.SeaweedStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class OutfitPipelineIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("fashionscape_pipeline")
            .withUsername("fsbot_test")
            .withPassword("fsbot_test");

    @Container
    static SeaweedContainer seaweed = new SeaweedContainer();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL config
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // SeaweedFS config
        registry.add("fsbot.seaweed.endpoint", seaweed::getS3URL);
        registry.add("fsbot.seaweed.access-key", seaweed::getAccessKey);
        registry.add("fsbot.seaweed.secret-key", seaweed::getSecretKey);
        registry.add("fsbot.seaweed.bucket", () -> "fashionscape-pipeline");
        registry.add("fsbot.seaweed.public-url", () -> "https://media.fashionscape.org");

        // Discord config
        registry.add("fsbot.discord.token", () -> "");
    }

    @Autowired
    private SeaweedStorageService storageService;

    @Autowired
    private OutfitRepository outfitRepository;

    @Autowired
    private GalleryChannelRepository galleryChannelRepository;

    @Autowired
    private BlocklistService blocklistService;

    @Autowired
    private OutfitService outfitService;

    @BeforeEach
    void setUp() throws Exception {
        storageService.prepareBucket();
        outfitRepository.deleteAll();
        galleryChannelRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-end pipeline: SeaweedFS asset upload and PostgreSQL persistence, retrieval, voting, and deletion")
    void testEndToEndOutfitPipeline() throws Exception {
        // 1. Configure a gallery channel
        String guildId = "guild-777";
        String channelId = "channel-888";
        GalleryChannelEntity galleryChannel = new GalleryChannelEntity(
                channelId,
                guildId,
                ":heart:",
                "retro",
                true
        );
        galleryChannelRepository.save(galleryChannel);
        assertTrue(galleryChannelRepository.existsByChannelIdAndEnabledTrue(channelId));

        // 2. Save outfit entity in PostgreSQL, which generates its UUID
        String submitterId = "user-999";
        OutfitEntity outfitEntity = outfitRepository.save(new OutfitEntity(
                null,
                "https://cdn.discordapp.com/attachments/1/2/pic.png",
                submitterId,
                galleryChannel.getTag(),
                "Retro dragon armor outfit",
                "RetroWarrior",
                Instant.now(),
                Instant.now(),
                false,
                false,
                0,
                null,
                "server-1",
                "channel-1"
        ));
        String outfitId = outfitEntity.getId();
        assertDoesNotThrow(() -> UUID.fromString(outfitId));
        String imageKey = "outfits/" + outfitId + ".png";

        // 3. Upload outfit image to SeaweedFS under that ID
        byte[] imageBytes = "fake-png-image-content".getBytes(StandardCharsets.UTF_8);
        UploadResult uploadResult = storageService.uploadOutfit(outfitId, imageBytes, "image/png");

        assertNotNull(uploadResult);
        assertEquals(imageKey, uploadResult.objectName());
        assertTrue(storageService.objectExists(imageKey));

        outfitEntity.setLink(uploadResult.publicUrl());
        outfitRepository.save(outfitEntity);

        // 4. Query and verify entity from PostgreSQL
        Optional<OutfitEntity> retrieved = outfitRepository.findByIdAndDeletedFalse(outfitId);
        assertTrue(retrieved.isPresent());
        OutfitEntity entity = retrieved.get();
        assertEquals(uploadResult.publicUrl(), entity.getLink());
        assertEquals("RetroWarrior", entity.getDiscordName());
        assertEquals("retro", entity.getTag());
        assertEquals(0, entity.getDisplayCount());
        assertFalse(entity.isDeleted());

        // Verify image bytes can be read back from SeaweedFS using public URL key extraction
        Optional<String> objectKey = storageService.extractObjectKey(entity.getLink());
        assertTrue(objectKey.isPresent());
        byte[] readBackBytes = storageService.getObjectBytes(objectKey.get());
        assertArrayEquals(imageBytes, readBackBytes);

        // 5. Upvote: one vote per user, counted in PostgreSQL
        assertEquals(Optional.of(new OutfitService.VoteResult(true, 1)), outfitService.toggleVote(outfitId, "voter-1"));

        OutfitEntity upvoted = outfitRepository.findByIdAndDeletedFalse(outfitId).orElseThrow();
        assertEquals(1, upvoted.getVoteCount());

        // 6. Test searching by tag and submitter
        List<OutfitEntity> tagMatches = outfitRepository.findByTagIgnoreCaseAndDeletedFalse("RETRO");
        assertEquals(1, tagMatches.size());
        assertEquals(outfitId, tagMatches.getFirst().getId());

        List<OutfitEntity> userMatches = outfitRepository.findBySubmitterAndDeletedFalse(submitterId);
        assertEquals(1, userMatches.size());
        assertEquals(outfitId, userMatches.getFirst().getId());

        // 7. Delete outfit: the image moves out of public read and the outfit is marked deleted
        assertTrue(outfitService.delete(outfitId, submitterId, guildId, null));

        // Verify it is no longer returned in active queries
        assertTrue(outfitRepository.findByIdAndDeletedFalse(outfitId).isEmpty());
        assertEquals(0, outfitRepository.countByDeletedFalse());
        assertTrue(outfitRepository.findByTagIgnoreCaseAndDeletedFalse("retro").isEmpty());

        OutfitEntity deleted = outfitRepository.findById(outfitId).orElseThrow();
        assertEquals(submitterId, deleted.getDeletedBy());
        assertNotNull(deleted.getDeletedAt());

        // The image is kept, but only under the quarantine prefix
        assertFalse(storageService.objectExists(imageKey));
        assertTrue(storageService.objectExists("deleted/" + imageKey));
        assertArrayEquals(imageBytes, outfitService.getDeletedImage(deleted).orElseThrow().data());

        // 8. Restore moves it back
        assertTrue(outfitService.restore(outfitId, "staff-1", guildId, null));
        assertTrue(outfitRepository.findByIdAndDeletedFalse(outfitId).isPresent());
        assertTrue(storageService.objectExists(imageKey));
        assertFalse(storageService.objectExists("deleted/" + imageKey));

        // Clean up SeaweedFS object
        storageService.deleteObject(imageKey);
        assertFalse(storageService.objectExists(imageKey));
    }

    @Test
    @DisplayName("A failed image upload leaves no outfit row behind")
    void testFailedUploadWritesNoOutfit() {
        OutfitEntity outfit = new OutfitEntity(null, "http://localhost:1/missing.png", "user-999", "retro", null,
                "RetroWarrior", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");

        assertThrows(Exception.class, () -> outfitService.submit(outfit, "http://localhost:1/missing.png"));
        assertEquals(0, outfitRepository.count());
    }
}
