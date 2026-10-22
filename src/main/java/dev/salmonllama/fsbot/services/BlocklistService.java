package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.repositories.BlockedServerRepository;
import dev.salmonllama.fsbot.database.repositories.BlockedUserRepository;
import org.springframework.stereotype.Service;

@Service
public class BlocklistService {

    private final BlockedUserRepository blockedUserRepository;
    private final BlockedServerRepository blockedServerRepository;

    public BlocklistService(BlockedUserRepository blockedUserRepository, BlockedServerRepository blockedServerRepository) {
        this.blockedUserRepository = blockedUserRepository;
        this.blockedServerRepository = blockedServerRepository;
    }

    public boolean isUserBlocked(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return blockedUserRepository.existsById(userId);
    }

    public boolean isServerBlocked(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return false;
        }
        return blockedServerRepository.existsById(serverId);
    }

    public boolean isBlocked(String userId, String serverId) {
        return isUserBlocked(userId) || isServerBlocked(serverId);
    }
}
