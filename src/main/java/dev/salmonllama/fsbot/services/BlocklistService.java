package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.BlockedServerEntity;
import dev.salmonllama.fsbot.database.entities.BlockedUserEntity;
import dev.salmonllama.fsbot.database.repositories.BlockedServerRepository;
import dev.salmonllama.fsbot.database.repositories.BlockedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BlocklistService {

    private static final Logger logger = LoggerFactory.getLogger(BlocklistService.class);

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

    @Transactional
    public BlockedUserEntity blockUser(String userId, String reason) {
        BlockedUserEntity entity = new BlockedUserEntity(userId, reason, Instant.now());
        BlockedUserEntity saved = blockedUserRepository.save(entity);
        logger.info("Blocked user ID {} with reason: {}", userId, reason);
        return saved;
    }

    @Transactional
    public boolean unblockUser(String userId) {
        if (!blockedUserRepository.existsById(userId)) {
            return false;
        }
        blockedUserRepository.deleteById(userId);
        logger.info("Unblocked user ID {}", userId);
        return true;
    }

    public Optional<BlockedUserEntity> getBlockedUser(String userId) {
        return blockedUserRepository.findById(userId);
    }

    public List<BlockedUserEntity> getAllBlockedUsers() {
        return blockedUserRepository.findAll();
    }

    @Transactional
    public BlockedServerEntity blockServer(String serverId, String name, String ownerId, String reason) {
        BlockedServerEntity entity = new BlockedServerEntity(serverId, name, ownerId, reason, Instant.now());
        BlockedServerEntity saved = blockedServerRepository.save(entity);
        logger.info("Blocked server ID {} ({}) with reason: {}", serverId, name, reason);
        return saved;
    }

    @Transactional
    public boolean unblockServer(String serverId) {
        if (!blockedServerRepository.existsById(serverId)) {
            return false;
        }
        blockedServerRepository.deleteById(serverId);
        logger.info("Unblocked server ID {}", serverId);
        return true;
    }

    public Optional<BlockedServerEntity> getBlockedServer(String serverId) {
        return blockedServerRepository.findById(serverId);
    }

    public List<BlockedServerEntity> getAllBlockedServers() {
        return blockedServerRepository.findAll();
    }
}
