package dev.salmonllama.fsbot.integration;

import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.GalleryChannelRepository;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.services.BlocklistService;
import dev.salmonllama.fsbot.storage.MinioStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class OutfitPipelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fashionscape_pipeline")
            .withUsername("fsbot_test")
            .withPassword("fsbot_test");

    @Container
    static MinIOContainer minio = new MinIOContainer(DockerImageName.parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .asCompatibleSubstituteFor("minio/minio"))
            .withUserName("minioadmin")
            .withPassword("minioadmin");

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

        // MinIO config
        registry.add("fsbot.minio.endpoint", minio::getS3URL);
        registry.add("fsbot.minio.access-key", minio::getUserName);
        registry.add("fsbot.minio.secret-key", minio::getPassword);
        registry.add("fsbot.minio.bucket", () -> "fashionscape-pipeline");
        registry.add("fsbot.minio.public-url", () -> "https://media.fashionscape.org");

        // Discord config
        registry.add("fsbot.discord.token", () -> "");
    }

    @Autowired
    private MinioStorageService minioStorageService;

    @Autowired
    private OutfitRepository outfitRepository;

    @Autowired
    private GalleryChannelRepository galleryChannelRepository;

    @Autowired
    private BlocklistService blocklistService;

    @BeforeEach
    void setUp() throws Exception {
        minioStorageService.ensureBucketExists();
        outfitRepository.deleteAll();
        galleryChannelRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-end pipeline: MinIO asset upload and PostgreSQL persistence, retrieval, voting, and deletion")
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

        // 2. Upload outfit image to MinIO
        String outfitId = "fit-pipeline-1";
        byte[] imageBytes = "fake-png-image-content".getBytes(StandardCharsets.UTF_8);
        UploadResult uploadResult = minioStorageService.uploadOutfit(outfitId, imageBytes, "image/png");

        assertNotNull(uploadResult);
        assertEquals("outfits/fit-pipeline-1.png", uploadResult.objectName());
        assertTrue(minioStorageService.objectExists("outfits/fit-pipeline-1.png"));

        // 3. Save outfit entity in PostgreSQL
        String submitterId = "user-999";
        OutfitEntity outfitEntity = new OutfitEntity(
                outfitId,
                uploadResult.publicUrl(),
                submitterId,
                galleryChannel.getTag(),
                "Retro dragon armor outfit",
                "RetroWarrior",
                Instant.now(),
                Instant.now(),
                false,
                false,
                0,
                null
        );
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

        // Verify image bytes can be read back from MinIO using public URL key extraction
        Optional<String> objectKey = minioStorageService.extractObjectKey(entity.getLink());
        assertTrue(objectKey.isPresent());
        byte[] readBackBytes = minioStorageService.getObjectBytes(objectKey.get());
        assertArrayEquals(imageBytes, readBackBytes);

        // 5. Simulate upvoting: increment count in PostgreSQL
        entity.setDisplayCount(entity.getDisplayCount() + 1);
        entity.setUpdated(Instant.now());
        outfitRepository.save(entity);

        OutfitEntity upvoted = outfitRepository.findByIdAndDeletedFalse(outfitId).orElseThrow();
        assertEquals(1, upvoted.getDisplayCount());

        // 6. Test searching by tag and submitter
        List<OutfitEntity> tagMatches = outfitRepository.findByTagIgnoreCaseAndDeletedFalse("RETRO");
        assertEquals(1, tagMatches.size());
        assertEquals(outfitId, tagMatches.getFirst().getId());

        List<OutfitEntity> userMatches = outfitRepository.findBySubmitterAndDeletedFalse(submitterId);
        assertEquals(1, userMatches.size());
        assertEquals(outfitId, userMatches.getFirst().getId());

        // 7. Soft delete outfit
        entity.setDeleted(true);
        entity.setUpdated(Instant.now());
        outfitRepository.save(entity);

        // Verify it is no longer returned in active queries
        assertTrue(outfitRepository.findByIdAndDeletedFalse(outfitId).isEmpty());
        assertEquals(0, outfitRepository.countByDeletedFalse());
        assertTrue(outfitRepository.findByTagIgnoreCaseAndDeletedFalse("retro").isEmpty());

        // Verify the asset remains in MinIO object storage for audit/recovery purposes
        assertTrue(minioStorageService.objectExists("outfits/fit-pipeline-1.png"));

        // Clean up MinIO object
        minioStorageService.deleteObject("outfits/fit-pipeline-1.png");
        assertFalse(minioStorageService.objectExists("outfits/fit-pipeline-1.png"));
    }
}
