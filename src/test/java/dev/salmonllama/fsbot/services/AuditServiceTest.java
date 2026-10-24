package dev.salmonllama.fsbot.services;

import dev.salmonllama.fsbot.database.entities.AuditLogEntity;
import dev.salmonllama.fsbot.database.entities.BotSettingEntity;
import dev.salmonllama.fsbot.database.repositories.AuditLogRepository;
import dev.salmonllama.fsbot.database.repositories.BotSettingRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private BotSettingRepository botSettingRepository;

    @Mock
    private JDA jda;

    @Mock
    private GuildMessageChannel auditChannel;

    @Mock
    private MessageCreateAction messageCreateAction;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, botSettingRepository);
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditChannel.sendMessageEmbeds(any(MessageEmbed.class))).thenReturn(messageCreateAction);
    }

    private void auditChannelConfigured() {
        when(botSettingRepository.findById(AuditService.AUDIT_CHANNEL_SETTING))
                .thenReturn(Optional.of(new BotSettingEntity(AuditService.AUDIT_CHANNEL_SETTING, "audit-chan")));
        when(jda.getChannelById(GuildMessageChannel.class, "audit-chan")).thenReturn(auditChannel);
    }

    @Test
    void recordSavesRowWithActorGuildAndTarget() {
        auditService.record(jda, AuditService.Action.USER_BLOCKED, "actor-1", "guild-1",
                AuditService.TARGET_USER, "target-1", "Reason: spam");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertEquals("USER_BLOCKED", saved.getAction());
        assertEquals("actor-1", saved.getActorId());
        assertEquals("guild-1", saved.getGuildId());
        assertEquals("user", saved.getTargetType());
        assertEquals("target-1", saved.getTargetId());
        assertEquals("Reason: spam", saved.getDetails());
        assertNotNull(saved.getCreated());
    }

    @Test
    void recordPostsToConfiguredChannel() {
        auditChannelConfigured();

        auditService.record(jda, AuditService.Action.OUTFIT_ADDED, "actor-1", "guild-1",
                AuditService.TARGET_OUTFIT, "outfit-1", null);

        verify(auditChannel).sendMessageEmbeds(any(MessageEmbed.class));
        verify(messageCreateAction).queue(any(), any());
    }

    @Test
    void recordWithoutChannelStillSavesAndPostsNothing() {
        when(botSettingRepository.findById(AuditService.AUDIT_CHANNEL_SETTING)).thenReturn(Optional.empty());

        auditService.record(jda, AuditService.Action.OUTFIT_ADDED, "actor-1", "guild-1",
                AuditService.TARGET_OUTFIT, "outfit-1", null);

        verify(auditLogRepository).save(any(AuditLogEntity.class));
        verify(jda, never()).getChannelById(eq(GuildMessageChannel.class), any(String.class));
    }

    @Test
    void discordFailureDoesNotThrowOrUndoTheRow() {
        auditChannelConfigured();
        when(auditChannel.sendMessageEmbeds(any(MessageEmbed.class))).thenThrow(new IllegalStateException("Discord is down"));

        AuditLogEntity entry = assertDoesNotThrow(() -> auditService.record(jda, AuditService.Action.OUTFIT_DELETED,
                "actor-1", "guild-1", AuditService.TARGET_OUTFIT, "outfit-1", null));

        assertNotNull(entry);
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    void missingChannelDoesNotThrow() {
        when(botSettingRepository.findById(AuditService.AUDIT_CHANNEL_SETTING))
                .thenReturn(Optional.of(new BotSettingEntity(AuditService.AUDIT_CHANNEL_SETTING, "gone-chan")));
        when(jda.getChannelById(GuildMessageChannel.class, "gone-chan")).thenReturn(null);

        assertDoesNotThrow(() -> auditService.record(jda, AuditService.Action.OUTFIT_DELETED,
                "actor-1", "guild-1", AuditService.TARGET_OUTFIT, "outfit-1", null));
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    void recordWithoutJdaOnlySaves() {
        auditService.record(null, AuditService.Action.OUTFIT_ADDED, "actor-1", "guild-1",
                AuditService.TARGET_OUTFIT, "outfit-1", null);

        verify(auditLogRepository).save(any(AuditLogEntity.class));
        verifyNoInteractions(botSettingRepository);
    }

    @Test
    void setAuditChannelUpdatesExistingSetting() {
        BotSettingEntity existing = new BotSettingEntity(AuditService.AUDIT_CHANNEL_SETTING, "old-chan");
        when(botSettingRepository.findById(AuditService.AUDIT_CHANNEL_SETTING)).thenReturn(Optional.of(existing));

        auditService.setAuditChannelId("new-chan");

        assertEquals("new-chan", existing.getValue());
        verify(botSettingRepository).save(existing);
    }
}
