package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.BlockedServerEntity;
import dev.salmonllama.fsbot.database.entities.BlockedUserEntity;
import dev.salmonllama.fsbot.database.repositories.BlockedServerRepository;
import dev.salmonllama.fsbot.database.repositories.BlockedUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void testBlockAndUnblockUser() {
        BlockedUserEntity user = new BlockedUserEntity("u123", "spam", Instant.now());
        when(blockedUserRepository.save(any())).thenReturn(user);
        when(blockedUserRepository.existsById("u123")).thenReturn(true);

        BlockedUserEntity result = blocklistService.blockUser("u123", "spam");
        assertEquals("u123", result.getId());
        assertEquals("spam", result.getReason());

        boolean unblocked = blocklistService.unblockUser("u123");
        assertTrue(unblocked);
        verify(blockedUserRepository).deleteById("u123");
    }

    @Test
    void testBlockAndUnblockServer() {
        BlockedServerEntity server = new BlockedServerEntity("s123", "guild", "owner", "raiding", Instant.now());
        when(blockedServerRepository.save(any())).thenReturn(server);
        when(blockedServerRepository.existsById("s123")).thenReturn(true);

        BlockedServerEntity result = blocklistService.blockServer("s123", "guild", "owner", "raiding");
        assertEquals("s123", result.getId());

        boolean unblocked = blocklistService.unblockServer("s123");
        assertTrue(unblocked);
        verify(blockedServerRepository).deleteById("s123");
    }

    @Test
    void testGetAllAndGetById() {
        BlockedUserEntity u = new BlockedUserEntity("u1", "r", Instant.now());
        when(blockedUserRepository.findById("u1")).thenReturn(Optional.of(u));
        when(blockedUserRepository.findAll()).thenReturn(List.of(u));

        assertTrue(blocklistService.getBlockedUser("u1").isPresent());
        assertEquals(1, blocklistService.getAllBlockedUsers().size());

        BlockedServerEntity s = new BlockedServerEntity("s1", "g", "o", "r", Instant.now());
        when(blockedServerRepository.findById("s1")).thenReturn(Optional.of(s));
        when(blockedServerRepository.findAll()).thenReturn(List.of(s));

        assertTrue(blocklistService.getBlockedServer("s1").isPresent());
        assertEquals(1, blocklistService.getAllBlockedServers().size());
    }
}
