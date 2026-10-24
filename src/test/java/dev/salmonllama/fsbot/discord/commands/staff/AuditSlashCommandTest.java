package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditSlashCommandTest {

    @Mock
    private StaticPermissionRepository staticPermissionRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SlashCommandInteractionEvent slashEvent;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    @Mock
    private User mockUser;

    @Mock
    private GuildChannelUnion channelUnion;

    @Mock
    private GuildMessageChannel channel;

    @Mock
    private Guild channelGuild;

    @Mock
    private Member selfMember;

    private AuditSlashCommand command;

    @BeforeEach
    void setUp() {
        BotProperties properties = new BotProperties(
                new BotProperties.DiscordProperties("token", 123456789L, "", "home-guild"),
                new BotProperties.SeaweedProperties("http://localhost:8333", "access", "secret", "bucket", "https://media.fashionscape.org")
        );
        command = new AuditSlashCommand(auditService, new PermissionService(staticPermissionRepository, properties));

        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("5555");
        when(staticPermissionRepository.existsByUserIdAndPermission("5555", "admin")).thenReturn(true);
        when(slashEvent.getSubcommandName()).thenReturn("config");
        when(slashEvent.getOption(eq("channel"), any())).thenReturn(channelUnion);
        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(anyBoolean())).thenReturn(replyCallbackAction);

        when(channelUnion.getType()).thenReturn(ChannelType.TEXT);
        when(channelUnion.asGuildMessageChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn("audit-chan");
        when(channel.getAsMention()).thenReturn("<#audit-chan>");
        when(channel.getGuild()).thenReturn(channelGuild);
        when(channel.canTalk()).thenReturn(true);
        when(channelGuild.getId()).thenReturn("home-guild");
        when(channelGuild.getSelfMember()).thenReturn(selfMember);
        when(selfMember.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
    }

    @Test
    void testCommandIsHomeGuildOnly() {
        assertEquals("audit", command.getName());
        assertTrue(command.isHomeGuildOnly());
        assertEquals(1, command.buildCommandData().getSubcommands().size());
    }

    @Test
    void testBotAdminSetsChannel() {
        command.execute(slashEvent);

        verify(auditService).setAuditChannelId("audit-chan");
        verify(auditService).record(any(), eq(AuditService.Action.AUDIT_CHANNEL_SET), eq("5555"), eq("home-guild"),
                eq(AuditService.TARGET_CHANNEL), eq("audit-chan"), any());
    }

    @Test
    void testOwnerSetsChannel() {
        when(mockUser.getId()).thenReturn("123456789");

        command.execute(slashEvent);

        verify(auditService).setAuditChannelId("audit-chan");
    }

    @Test
    void testBotStaffIsRefused() {
        when(mockUser.getId()).thenReturn("6666");
        when(staticPermissionRepository.existsByUserIdAndPermission("6666", "staff")).thenReturn(true);

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("Only the bot owner and bot admins"));
        verifyNoInteractions(auditService);
    }

    @Test
    void testChannelOutsideHomeServerIsRefused() {
        when(channelGuild.getId()).thenReturn("other-guild");

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("must be in the home server"));
        verify(auditService, never()).setAuditChannelId(any());
    }

    @Test
    void testChannelBotCannotPostInIsRefused() {
        when(channel.canTalk()).thenReturn(false);

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("can't post embeds"));
        verify(auditService, never()).setAuditChannelId(any());
    }
}
