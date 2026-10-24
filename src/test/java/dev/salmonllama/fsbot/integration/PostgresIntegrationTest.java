package dev.salmonllama.fsbot.integration;

import dev.salmonllama.fsbot.database.entities.*;
import dev.salmonllama.fsbot.database.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class PostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fashionscape_test")
            .withUsername("fsbot_test")
            .withPassword("fsbot_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("fsbot.discord.token", () -> "");
    }

    @Autowired
    private OutfitRepository outfitRepository;

    @Autowired
    private GalleryChannelRepository galleryChannelRepository;

    @Autowired
    private ServerConfigRepository serverConfigRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private BlockedServerRepository blockedServerRepository;

    @BeforeEach
    void setUp() {
        outfitRepository.deleteAll();
        galleryChannelRepository.deleteAll();
        serverConfigRepository.deleteAll();
        blockedUserRepository.deleteAll();
        blockedServerRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify PostgreSQL flyway schema and OutfitRepository CRUD with native random query")
    void testOutfitRepositoryPostgres() {
        OutfitEntity outfit1 = new OutfitEntity(
                "pg-outfit-1",
                "https://media.fashionscape.org/outfits/pg-outfit-1.png",
                "111222333444",
                "saradomin",
                "Blue and gold paladin",
                "SaradominKnight",
                Instant.now(),
                Instant.now(),
                false,
                true,
                10,
                "del-hash-1",
                "server-1",
                "channel-1"
        );

        OutfitEntity outfit2 = new OutfitEntity(
                "pg-outfit-2",
                "https://media.fashionscape.org/outfits/pg-outfit-2.png",
                "222333444555",
                "zamorak",
                "Red and black battlemage",
                "ZammyMage",
                Instant.now(),
                Instant.now(),
                false,
                false,
                3,
                "del-hash-2",
                "server-1",
                "channel-1"
        );

        OutfitEntity deletedOutfit = new OutfitEntity(
                "pg-outfit-3",
                "https://media.fashionscape.org/outfits/pg-outfit-3.png",
                "111222333444",
                "saradomin",
                "Old outfit",
                "SaradominKnight",
                Instant.now(),
                Instant.now(),
                true,
                false,
                0,
                null,
                "server-1",
                "channel-1"
        );

        outfitRepository.saveAll(List.of(outfit1, outfit2, deletedOutfit));

        assertEquals(2, outfitRepository.countByDeletedFalse());

        Optional<OutfitEntity> found = outfitRepository.findByIdAndDeletedFalse("pg-outfit-1");
        assertTrue(found.isPresent());
        assertEquals("saradomin", found.get().getTag());
        assertEquals("SaradominKnight", found.get().getDiscordName());

        Optional<OutfitEntity> deleted = outfitRepository.findByIdAndDeletedFalse("pg-outfit-3");
        assertTrue(deleted.isEmpty());

        List<OutfitEntity> bySubmitter = outfitRepository.findBySubmitterAndDeletedFalse("111222333444");
        assertEquals(1, bySubmitter.size());
        assertEquals("pg-outfit-1", bySubmitter.getFirst().getId());

        List<OutfitEntity> byTag = outfitRepository.findByTagIgnoreCaseAndDeletedFalse("SARADOMIN");
        assertEquals(1, byTag.size());
        assertEquals("pg-outfit-1", byTag.getFirst().getId());

        // Test native Postgres random query
        Optional<OutfitEntity> random = outfitRepository.findRandomOutfit();
        assertTrue(random.isPresent());
        assertFalse(random.get().isDeleted());
    }

    @Test
    @DisplayName("Verify GalleryChannelRepository operations against PostgreSQL")
    void testGalleryChannelRepositoryPostgres() {
        GalleryChannelEntity channel = new GalleryChannelEntity(
                "123456789",
                "999888777",
                ":heart:",
                "fashion",
                true
        );

        galleryChannelRepository.save(channel);

        List<GalleryChannelEntity> guildGalleries = galleryChannelRepository.findByServerId("999888777");
        assertEquals(1, guildGalleries.size());

        Optional<GalleryChannelEntity> active = galleryChannelRepository.findByChannelIdAndEnabledTrue("123456789");
        assertTrue(active.isPresent());
        assertEquals("fashion", active.get().getTag());
    }

    @Test
    @DisplayName("Verify ServerConfigRepository operations against PostgreSQL")
    void testServerConfigRepositoryPostgres() {
        ServerConfigEntity config = new ServerConfigEntity(
                "999888777",
                "~",
                "Welcome to FashionScape!",
                "111222333"
        );

        serverConfigRepository.save(config);

        Optional<ServerConfigEntity> found = serverConfigRepository.findById("999888777");
        assertTrue(found.isPresent());
        assertEquals("~", found.get().getPrefix());
        assertEquals("Welcome to FashionScape!", found.get().getWelcomeMessage());
        assertEquals("111222333", found.get().getWelcomeChannel());
    }

    @Test
    @DisplayName("Verify BlockedUser and BlockedServer entities against PostgreSQL")
    void testBlockedEntitiesPostgres() {
        BlockedUserEntity user = new BlockedUserEntity(
                "444555666",
                "Toxicity",
                Instant.now()
        );
        blockedUserRepository.save(user);

        BlockedServerEntity server = new BlockedServerEntity(
                "888777666",
                "Spam guild",
                "999000111",
                "Spamming",
                Instant.now()
        );
        blockedServerRepository.save(server);

        assertTrue(blockedUserRepository.existsById("444555666"));
        assertFalse(blockedUserRepository.existsById("000000000"));

        assertTrue(blockedServerRepository.existsById("888777666"));
        assertFalse(blockedServerRepository.existsById("000000000"));
    }
}
