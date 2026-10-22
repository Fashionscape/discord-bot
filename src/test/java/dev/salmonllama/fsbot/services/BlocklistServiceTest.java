package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.repositories.BlockedServerRepository;
import dev.salmonllama.fsbot.database.repositories.BlockedUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlocklistServiceTest {

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private BlockedServerRepository blockedServerRepository;

    private BlocklistService blocklistService;

    @BeforeEach
    void setUp() {
        blocklistService = new BlocklistService(blockedUserRepository, blockedServerRepository);
    }

    @Test
    void testIsUserBlocked() {
        when(blockedUserRepository.existsById("bad-user")).thenReturn(true);
        when(blockedUserRepository.existsById("good-user")).thenReturn(false);

        assertTrue(blocklistService.isUserBlocked("bad-user"));
        assertFalse(blocklistService.isUserBlocked("good-user"));
        assertFalse(blocklistService.isUserBlocked(null));
        assertFalse(blocklistService.isUserBlocked(""));
    }

    @Test
    void testIsServerBlocked() {
        when(blockedServerRepository.existsById("bad-server")).thenReturn(true);
        when(blockedServerRepository.existsById("good-server")).thenReturn(false);

        assertTrue(blocklistService.isServerBlocked("bad-server"));
        assertFalse(blocklistService.isServerBlocked("good-server"));
        assertFalse(blocklistService.isServerBlocked(null));
        assertFalse(blocklistService.isServerBlocked("   "));
    }

    @Test
    void testIsBlocked() {
        when(blockedUserRepository.existsById("bad-user")).thenReturn(true);
        when(blockedServerRepository.existsById("bad-server")).thenReturn(true);

        assertTrue(blocklistService.isBlocked("bad-user", "good-server"));
        assertTrue(blocklistService.isBlocked("good-user", "bad-server"));
        assertFalse(blocklistService.isBlocked("good-user", "good-server"));
    }
}
