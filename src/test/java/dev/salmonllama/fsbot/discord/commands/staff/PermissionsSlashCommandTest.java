package dev.salmonllama.fsbot.discord.commands.staff;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.StaticPermissionEntity;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionsSlashCommandTest {

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

    private PermissionsSlashCommand command;

    @BeforeEach
    void setUp() {
        BotProperties properties = new BotProperties(
                new BotProperties.DiscordProperties("token", 123456789L, "", "home-guild"),
                new BotProperties.SeaweedProperties("http://localhost:8333", "access", "secret", "bucket", "https://media.fashionscape.org")
        );
        command = new PermissionsSlashCommand(new PermissionService(staticPermissionRepository, properties), auditService);

        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("123456789"); // bot owner
        when(targetUser.getId()).thenReturn("4001");
        when(targetUser.getAsMention()).thenReturn("<@4001>");
        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(slashEvent.replyEmbeds(any(MessageEmbed.class))).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(anyBoolean())).thenReturn(replyCallbackAction);
    }

    @Test
    void testCommandData() {
        assertEquals("permissions", command.getName());
        assertTrue(command.isHomeGuildOnly());

        SlashCommandData data = command.buildCommandData();
        assertEquals(3, data.getSubcommands().size());
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("grant")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("revoke")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("list")));
    }

    @Test
    void testBotAdminIsRefused() {
        when(mockUser.getId()).thenReturn("5555");
        when(staticPermissionRepository.existsByUserIdAndPermission("5555", "admin")).thenReturn(true);
        when(slashEvent.getSubcommandName()).thenReturn("grant");

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("Only the bot owner"));
        verify(staticPermissionRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void testGrant() {
        when(slashEvent.getSubcommandName()).thenReturn("grant");
        when(slashEvent.getOption(eq("user"), any())).thenReturn(targetUser);
        when(slashEvent.getOption(eq("type"), any())).thenReturn("staff");

        command.execute(slashEvent);

        ArgumentCaptor<StaticPermissionEntity> captor = ArgumentCaptor.forClass(StaticPermissionEntity.class);
        verify(staticPermissionRepository).save(captor.capture());
        assertEquals("4001", captor.getValue().getUserId());
        assertEquals("staff", captor.getValue().getPermission());
        verify(slashEvent).reply(contains("Granted `staff`"));
        verify(auditService).record(any(), eq(AuditService.Action.PERMISSION_GRANTED), eq("123456789"), any(),
                eq(AuditService.TARGET_USER), eq("4001"), any());
    }

    @Test
    void testGrantExistingPermissionSavesNothing() {
        when(slashEvent.getSubcommandName()).thenReturn("grant");
        when(slashEvent.getOption(eq("user"), any())).thenReturn(targetUser);
        when(slashEvent.getOption(eq("type"), any())).thenReturn("admin");
        when(staticPermissionRepository.existsByUserIdAndPermission("4001", "admin")).thenReturn(true);

        command.execute(slashEvent);

        verify(staticPermissionRepository, never()).save(any());
        verify(slashEvent).reply(contains("already has"));
    }

    @Test
    void testRevoke() {
        when(slashEvent.getSubcommandName()).thenReturn("revoke");
        when(slashEvent.getOption(eq("user"), any())).thenReturn(targetUser);
        when(slashEvent.getOption(eq("type"), any())).thenReturn("admin");
        when(staticPermissionRepository.existsByUserIdAndPermission("4001", "admin")).thenReturn(true);

        command.execute(slashEvent);

        verify(staticPermissionRepository).deleteByUserIdAndPermission("4001", "admin");
        verify(slashEvent).reply(contains("Revoked `admin`"));
        verify(auditService).record(any(), eq(AuditService.Action.PERMISSION_REVOKED), eq("123456789"), any(),
                eq(AuditService.TARGET_USER), eq("4001"), any());
    }

    @Test
    void testListEmpty() {
        when(slashEvent.getSubcommandName()).thenReturn("list");
        when(staticPermissionRepository.findAll(any(Sort.class))).thenReturn(List.of());

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("No bot permissions are set"));
        verify(auditService).record(any(), eq(AuditService.Action.PERMISSIONS_VIEWED), eq("123456789"), any(), any(), any(), any());
    }

    @Test
    void testListAll() {
        when(slashEvent.getSubcommandName()).thenReturn("list");
        when(staticPermissionRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(new StaticPermissionEntity("4001", "staff", Instant.now())));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> captor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(slashEvent).replyEmbeds(captor.capture());
        assertEquals("Bot Permissions (1)", captor.getValue().getTitle());
    }
}
