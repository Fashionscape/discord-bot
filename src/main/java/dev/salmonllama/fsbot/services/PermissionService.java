package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.entities.StaticPermissionEntity;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single source of truth for who may do what.
 * <p>
 * Bot-level roles (owner, bot admin, bot staff) are global. A server's own Discord
 * permissions only count for outfits that were submitted in that server.
 */
@Service
public class PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    public static final String ADMIN = "admin";
    public static final String STAFF = "staff";
    public static final Set<String> PERMISSION_TYPES = Set.of(ADMIN, STAFF);

    private final StaticPermissionRepository staticPermissionRepository;
    private final BotProperties properties;

    public PermissionService(StaticPermissionRepository staticPermissionRepository, BotProperties properties) {
        this.staticPermissionRepository = staticPermissionRepository;
        this.properties = properties;
    }

    public boolean isOwner(String userId) {
        if (userId == null || properties.discord() == null || properties.discord().ownerId() <= 0) {
            return false;
        }
        return String.valueOf(properties.discord().ownerId()).equals(userId);
    }

    public boolean isBotAdmin(String userId) {
        if (userId == null) {
            return false;
        }
        return isOwner(userId) || staticPermissionRepository.existsByUserIdAndPermission(userId, ADMIN);
    }

    public boolean isBotStaff(String userId) {
        if (userId == null) {
            return false;
        }
        return isBotAdmin(userId) || staticPermissionRepository.existsByUserIdAndPermission(userId, STAFF);
    }

    /**
     * Whether the user may delete the outfit: its submitter, bot staff, or a moderator
     * of the server the outfit was submitted in.
     */
    public boolean canModerateOutfit(String userId, Member member, OutfitEntity outfit) {
        if (userId == null || outfit == null) {
            return false;
        }
        if (userId.equals(outfit.getSubmitter()) || isBotStaff(userId)) {
            return true;
        }
        return isGuildModerator(member) && outfit.getServerId() != null
                && outfit.getServerId().equals(member.getGuild().getId());
    }

    public boolean isHomeGuild(String guildId) {
        String homeGuildId = homeGuildId();
        return guildId != null && !homeGuildId.isBlank() && homeGuildId.equals(guildId);
    }

    public String homeGuildId() {
        if (properties.discord() == null || properties.discord().homeGuildId() == null) {
            return "";
        }
        return properties.discord().homeGuildId();
    }

    public static String normalizePermissionType(String permission) {
        if (permission == null) {
            return null;
        }
        String normalized = permission.trim().toLowerCase(Locale.ROOT);
        return PERMISSION_TYPES.contains(normalized) ? normalized : null;
    }

    /**
     * Grants a static permission. Returns false if the user already holds it.
     */
    @Transactional
    public boolean grant(String userId, String permission) {
        if (staticPermissionRepository.existsByUserIdAndPermission(userId, permission)) {
            return false;
        }
        staticPermissionRepository.save(new StaticPermissionEntity(userId, permission, Instant.now()));
        logger.info("Granted static permission {} to user {}", permission, userId);
        return true;
    }

    /**
     * Revokes a static permission. Returns false if the user did not hold it.
     */
    @Transactional
    public boolean revoke(String userId, String permission) {
        if (!staticPermissionRepository.existsByUserIdAndPermission(userId, permission)) {
            return false;
        }
        staticPermissionRepository.deleteByUserIdAndPermission(userId, permission);
        logger.info("Revoked static permission {} from user {}", permission, userId);
        return true;
    }

    public List<StaticPermissionEntity> list(String userId) {
        if (userId == null) {
            return staticPermissionRepository.findAll(Sort.by("userId", "permission"));
        }
        return staticPermissionRepository.findByUserId(userId);
    }

    private static boolean isGuildModerator(Member member) {
        return member != null && (
                member.hasPermission(Permission.ADMINISTRATOR) ||
                member.hasPermission(Permission.MANAGE_SERVER) ||
                member.hasPermission(Permission.MESSAGE_MANAGE)
        );
    }
}
