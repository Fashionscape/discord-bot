package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.BlockedServerEntity;
import dev.salmonllama.fsbot.database.entities.BlockedUserEntity;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import dev.salmonllama.fsbot.services.BlocklistService;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlockSlashCommandTest {

    @Mock
    private BlocklistService blocklistService;

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
    private User targetUser;

    @Mock
    private Member mockMember;

    private BotProperties properties;
    private BlockSlashCommand command;

    @BeforeEach
    void setUp() {
        properties = new BotProperties(
                new BotProperties.DiscordProperties("token", 123456789L, "dev-guild", "home-guild"),
                new BotProperties.SeaweedProperties("http://localhost:8333", "access", "secret", "bucket", "https://media.fashionscape.org")
        );
        command = new BlockSlashCommand(blocklistService, new PermissionService(staticPermissionRepository, properties), auditService);

        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("123456789"); // bot owner
        when(mockUser.getAsTag()).thenReturn("Admin#0001");
        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(slashEvent.replyEmbeds(any(MessageEmbed.class))).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(anyBoolean())).thenReturn(replyCallbackAction);
    }

    @Test
    void testCommandData() {
        assertEquals("block", command.getName());
        assertNotNull(command.getDescription());

        SlashCommandData data = command.buildCommandData();
        assertEquals("block", data.getName());
        assertEquals(3, data.getSubcommands().size());
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("user")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("server")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("list")));
        assertTrue(command.isHomeGuildOnly());
    }

    @Test
    void testUnauthorized() {
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(Permission.ADMINISTRATOR)).thenReturn(false);

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("do not have permission"));
    }

    @Test
    void testAdministratorOfAnotherServerRefused() {
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(any(Permission.class))).thenReturn(true);
        when(slashEvent.getSubcommandName()).thenReturn("user");

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("do not have permission"));
        verifyNoInteractions(blocklistService, auditService);
    }

    @Test
    void testBotAdminAllowed() {
        when(mockUser.getId()).thenReturn("5555");
        when(staticPermissionRepository.existsByUserIdAndPermission("5555", "admin")).thenReturn(true);
        when(slashEvent.getSubcommandName()).thenReturn("list");
        when(slashEvent.getOption(eq("type"), any())).thenReturn("users");
        when(blocklistService.getAllBlockedUsers()).thenReturn(List.of());

        command.execute(slashEvent);

        verify(blocklistService).getAllBlockedUsers();
    }

    @Test
    void testCannotBlockOwnerOrBotAdmin() {
        when(slashEvent.getSubcommandName()).thenReturn("user");
        when(slashEvent.getOption(eq("action"), any())).thenReturn("block");
        when(slashEvent.getOption(eq("user"), any())).thenReturn(targetUser);
        when(targetUser.getId()).thenReturn("123456789"); // the owner

        command.execute(slashEvent);

        when(targetUser.getId()).thenReturn("5555");
        when(staticPermissionRepository.existsByUserIdAndPermission("5555", "admin")).thenReturn(true);

        command.execute(slashEvent);

        verify(blocklistService, never()).blockUser(anyString(), any());
        verify(slashEvent, times(2)).reply(contains("can't be blocked"));
    }

    @Test
    void testCannotBlockHomeServer() {
        when(slashEvent.getSubcommandName()).thenReturn("server");
        when(slashEvent.getOption(eq("action"), any())).thenReturn("block");
        when(slashEvent.getOption(eq("server_id"), any())).thenReturn("home-guild");

        command.execute(slashEvent);

        verify(blocklistService, never()).blockServer(anyString(), any(), any(), any());
        verify(slashEvent).reply(contains("home server can't be blocked"));
    }

    @Test
    void testUserBlockAndUnblock() {
        when(slashEvent.getSubcommandName()).thenReturn("user");
        when(slashEvent.getOption(eq("action"), any())).thenReturn("block");
        when(slashEvent.getOption(eq("user"), any())).thenReturn(targetUser);
        when(targetUser.getId()).thenReturn("4001");
        when(targetUser.getAsMention()).thenReturn("<@4001>");
        when(targetUser.getAsTag()).thenReturn("BadActor#1234");
        when(blocklistService.isUserBlocked("4001")).thenReturn(false);

        command.execute(slashEvent);

        verify(blocklistService).blockUser(eq("4001"), any());
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));
        verify(auditService).record(any(), eq(AuditService.Action.USER_BLOCKED), eq("123456789"), any(),
                eq(AuditService.TARGET_USER), eq("4001"), any());

        // Test unblock
        when(slashEvent.getOption(eq("action"), any())).thenReturn("unblock");
        when(blocklistService.isUserBlocked("4001")).thenReturn(true);

        command.execute(slashEvent);

        verify(blocklistService).unblockUser("4001");
        verify(auditService).record(any(), eq(AuditService.Action.USER_UNBLOCKED), eq("123456789"), any(),
                eq(AuditService.TARGET_USER), eq("4001"), any());
    }

    @Test
    void testServerBlockAndUnblock() {
        when(slashEvent.getSubcommandName()).thenReturn("server");
        when(slashEvent.getOption(eq("action"), any())).thenReturn("block");
        when(slashEvent.getOption(eq("server_id"), any())).thenReturn("server-99");
        when(blocklistService.isServerBlocked("server-99")).thenReturn(false);

        command.execute(slashEvent);

        verify(blocklistService).blockServer(eq("server-99"), isNull(), isNull(), any());
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));

        // Test unblock
        when(slashEvent.getOption(eq("action"), any())).thenReturn("unblock");
        when(blocklistService.isServerBlocked("server-99")).thenReturn(true);

        command.execute(slashEvent);

        verify(blocklistService).unblockServer("server-99");
    }

    @Test
    void testListBlocked() {
        when(slashEvent.getSubcommandName()).thenReturn("list");
        when(slashEvent.getOption(eq("type"), any())).thenReturn("users");

        BlockedUserEntity u = new BlockedUserEntity("u1", "spammer", Instant.now());
        when(blocklistService.getAllBlockedUsers()).thenReturn(List.of(u));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> captor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(slashEvent).replyEmbeds(captor.capture());
        MessageEmbed embed = captor.getValue();
        assertEquals("Blocked Users (1)", embed.getTitle());
        verify(auditService).record(any(), eq(AuditService.Action.BLOCKLIST_VIEWED), eq("123456789"), any(), any(), any(), any());
    }
}
