package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.AuditLogEntity;
import dev.salmonllama.fsbot.database.entities.BotSettingEntity;
import dev.salmonllama.fsbot.database.repositories.AuditLogRepository;
import dev.salmonllama.fsbot.database.repositories.BotSettingRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.awt.Color;
import java.time.Instant;
import java.util.Optional;

/**
 * Records staff actions and outfit changes in the audit_log table and posts each one to the
 * configured audit channel.
 * <p>
 * The row is saved in the caller's transaction, so it is rolled back with the action. The
 * channel post happens after commit and is best-effort: a Discord failure is logged and never
 * undoes the row or the action.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final Color EMBED_COLOR = new Color(0xFEE75C);

    public static final String AUDIT_CHANNEL_SETTING = "audit_channel_id";

    public static final String TARGET_OUTFIT = "outfit";
    public static final String TARGET_USER = "user";
    public static final String TARGET_SERVER = "server";
    public static final String TARGET_CHANNEL = "channel";

    public enum Action {
        OUTFIT_ADDED,
        OUTFIT_DELETED,
        OUTFIT_RESTORED,
        USER_BLOCKED,
        USER_UNBLOCKED,
        USER_BLOCK_CHECKED,
        SERVER_BLOCKED,
        SERVER_UNBLOCKED,
        SERVER_BLOCK_CHECKED,
        BLOCKLIST_VIEWED,
        PERMISSION_GRANTED,
        PERMISSION_REVOKED,
        PERMISSIONS_VIEWED,
        AUDIT_CHANNEL_SET,
        GALLERY_ADDED,
        GALLERY_REMOVED
    }

    private final AuditLogRepository auditLogRepository;
    private final BotSettingRepository botSettingRepository;

    public AuditService(AuditLogRepository auditLogRepository, BotSettingRepository botSettingRepository) {
        this.auditLogRepository = auditLogRepository;
        this.botSettingRepository = botSettingRepository;
    }

    /**
     * Saves an audit row and schedules its audit channel post.
     *
     * @param jda used to post to the audit channel; may be null, in which case nothing is posted
     */
    @Transactional
    public AuditLogEntity record(JDA jda, Action action, String actorId, String guildId,
                                 String targetType, String targetId, String details) {
        AuditLogEntity entry = auditLogRepository.save(
                new AuditLogEntity(action.name(), actorId, guildId, targetType, targetId, details, Instant.now())
        );
        logger.info("Audit: {} by {} in guild {} on {} {}", action, actorId, guildId, targetType, targetId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    post(jda, entry);
                }
            });
        } else {
            post(jda, entry);
        }
        return entry;
    }

    public Optional<String> getAuditChannelId() {
        return botSettingRepository.findById(AUDIT_CHANNEL_SETTING).map(BotSettingEntity::getValue);
    }

    @Transactional
    public void setAuditChannelId(String channelId) {
        BotSettingEntity setting = botSettingRepository.findById(AUDIT_CHANNEL_SETTING)
                .orElseGet(() -> new BotSettingEntity(AUDIT_CHANNEL_SETTING, channelId));
        setting.setValue(channelId);
        botSettingRepository.save(setting);
    }

    void post(JDA jda, AuditLogEntity entry) {
        if (jda == null) {
            return;
        }
        try {
            Optional<String> channelId = getAuditChannelId();
            if (channelId.isEmpty()) {
                return;
            }

            GuildMessageChannel channel = jda.getChannelById(GuildMessageChannel.class, channelId.get());
            if (channel == null) {
                logger.warn("Audit channel {} was not found; audit entry {} was saved but not posted", channelId.get(), entry.getId());
                return;
            }

            channel.sendMessageEmbeds(buildEmbed(entry)).queue(
                    null,
                    error -> logger.warn("Failed to post audit entry {} to channel {}", entry.getId(), channelId.get(), error)
            );
        } catch (Exception e) {
            logger.warn("Failed to post audit entry {}", entry.getId(), e);
        }
    }

    static MessageEmbed buildEmbed(AuditLogEntity entry) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(entry.getAction())
                .setColor(EMBED_COLOR)
                .addField("Actor", "<@" + entry.getActorId() + "> (`" + entry.getActorId() + "`)", true)
                .addField("Server", entry.getGuildId() != null ? "`" + entry.getGuildId() + "`" : "Direct message", true)
                .setTimestamp(entry.getCreated())
                .setFooter("Audit entry #" + entry.getId());

        if (entry.getTargetType() != null) {
            embed.addField("Target", entry.getTargetType() + " `" + entry.getTargetId() + "`", true);
        }
        if (entry.getDetails() != null && !entry.getDetails().isBlank()) {
            embed.setDescription(entry.getDetails());
        }
        return embed.build();
    }
}
