package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OutfitRepositoryTest {

    @Autowired
    private OutfitRepository outfitRepository;

    private String outfit1Id;
    private String deletedOutfitId;

    @BeforeEach
    void setUp() {
        outfitRepository.deleteAll();

        OutfitEntity outfit1 = new OutfitEntity(
                null,
                "https://media.fashionscape.org/outfit1.png",
                "user-100",
                "zammy",
                "meta info",
                "Zamorakian",
                Instant.now(),
                Instant.now(),
                false,
                true,
                5,
                "del-hash-1",
                "server-1",
                "channel-1"
        );

        OutfitEntity outfit2 = new OutfitEntity(
                null,
                "https://media.fashionscape.org/outfit2.png",
                "user-200",
                "sara",
                null,
                "Saradominist",
                Instant.now(),
                Instant.now(),
                false,
                false,
                2,
                "del-hash-2",
                "server-1",
                "channel-1"
        );

        OutfitEntity deletedOutfit = new OutfitEntity(
                null,
                "https://media.fashionscape.org/deleted.png",
                "user-100",
                "guthix",
                null,
                "Guthixian",
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
        outfit1Id = outfit1.getId();
        deletedOutfitId = deletedOutfit.getId();
    }

    @Test
    void testFindByDeletedFalse() {
        List<OutfitEntity> activeOutfits = outfitRepository.findByDeletedFalse();
        assertEquals(2, activeOutfits.size());
    }

    @Test
    void testFindByIdAndDeletedFalse() {
        Optional<OutfitEntity> outfit = outfitRepository.findByIdAndDeletedFalse(outfit1Id);
        assertTrue(outfit.isPresent());
        assertEquals("user-100", outfit.get().getSubmitter());

        Optional<OutfitEntity> deleted = outfitRepository.findByIdAndDeletedFalse(deletedOutfitId);
        assertFalse(deleted.isPresent());
    }

    @Test
    void testFindBySubmitterAndDeletedFalse() {
        List<OutfitEntity> outfits = outfitRepository.findBySubmitterAndDeletedFalse("user-100");
        assertEquals(1, outfits.size());
        assertEquals(outfit1Id, outfits.get(0).getId());
    }

    @Test
    void testFindByTagIgnoreCaseAndDeletedFalse() {
        List<OutfitEntity> outfits = outfitRepository.findByTagIgnoreCaseAndDeletedFalse("ZAMMY");
        assertEquals(1, outfits.size());
        assertEquals(outfit1Id, outfits.get(0).getId());
    }

    @Test
    void testFindRandomOutfit() {
        Optional<OutfitEntity> random = outfitRepository.findRandomOutfit();
        assertTrue(random.isPresent());
        assertFalse(random.get().isDeleted());
    }

    @Test
    void testCountByDeletedFalse() {
        assertEquals(2, outfitRepository.countByDeletedFalse());
    }

    @Test
    void testOutfitRequiresServerAndChannelIds() {
        assertThrows(NullPointerException.class, () -> new OutfitEntity(
                "no-server", "https://example.com/x.png", "user-1", "osrs", null, "Name",
                Instant.now(), Instant.now(), false, false, 0, null, null, "channel-1"));
        assertThrows(NullPointerException.class, () -> new OutfitEntity(
                "no-channel", "https://example.com/x.png", "user-1", "osrs", null, "Name",
                Instant.now(), Instant.now(), false, false, 0, null, "server-1", null));
    }

    @Test
    void testSaveGeneratesUuid() {
        assertDoesNotThrow(() -> UUID.fromString(outfit1Id));
        assertNotEquals(outfit1Id, deletedOutfitId);
    }

    @Test
    void testDatabaseRejectsOutfitWithoutServerId() {
        OutfitEntity outfit = new OutfitEntity();
        outfit.setLink("https://example.com/orphan.png");
        outfit.setSubmitter("user-1");
        outfit.setChannelId("channel-1");

        assertThrows(DataIntegrityViolationException.class, () -> outfitRepository.saveAndFlush(outfit));
    }

    @Test
    void testDatabaseRejectsOutfitWithoutChannelId() {
        OutfitEntity outfit = new OutfitEntity();
        outfit.setLink("https://example.com/orphan.png");
        outfit.setSubmitter("user-1");
        outfit.setServerId("server-1");

        assertThrows(DataIntegrityViolationException.class, () -> outfitRepository.saveAndFlush(outfit));
    }
}
