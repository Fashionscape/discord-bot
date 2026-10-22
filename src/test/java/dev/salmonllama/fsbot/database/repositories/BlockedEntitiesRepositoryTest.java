package dev.salmonllama.fsbot.database.repositories;

import dev.salmonllama.fsbot.database.entities.BlockedServerEntity;
import dev.salmonllama.fsbot.database.entities.BlockedUserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BlockedEntitiesRepositoryTest {

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private BlockedServerRepository blockedServerRepository;

    @BeforeEach
    void setUp() {
        blockedUserRepository.deleteAll();
        blockedServerRepository.deleteAll();

        blockedUserRepository.save(new BlockedUserEntity("user-bad-1", "Spamming gallery", Instant.now()));
        blockedServerRepository.save(new BlockedServerEntity("server-bad-1", "Malicious Guild", "owner-99", "Toxicity", Instant.now()));
    }

    @Test
    void testBlockedUserOperations() {
        assertTrue(blockedUserRepository.existsById("user-bad-1"));
        assertFalse(blockedUserRepository.existsById("user-good-1"));

        Optional<BlockedUserEntity> entity = blockedUserRepository.findById("user-bad-1");
        assertTrue(entity.isPresent());
        assertEquals("Spamming gallery", entity.get().getReason());
    }

    @Test
    void testBlockedServerOperations() {
        assertTrue(blockedServerRepository.existsById("server-bad-1"));
        assertFalse(blockedServerRepository.existsById("server-good-1"));

        Optional<BlockedServerEntity> entity = blockedServerRepository.findById("server-bad-1");
        assertTrue(entity.isPresent());
        assertEquals("Malicious Guild", entity.get().getName());
        assertEquals("owner-99", entity.get().getOwnerId());
    }
}
