package dev.salmonllama.fsbot.discord.commands.general;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import dev.salmonllama.fsbot.services.OutfitService;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutfitSlashCommandTest {

    @Mock
    private OutfitService outfitService;

    @Mock
    private StaticPermissionRepository staticPermissionRepository;

    @Mock
    private SlashCommandInteractionEvent slashEvent;

    @Mock
    private ButtonInteractionEvent buttonEvent;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    @Mock
    private MessageEditCallbackAction messageEditCallbackAction;

    @Mock
    private User mockUser;

    @Mock
    private Member mockMember;

    @Mock
    private OptionMapping mockOptionMapping;

    @Mock
    private Guild mockGuild;

    private BotProperties properties;
    private OutfitSlashCommand command;

    @BeforeEach
    void setUp() {
        properties = new BotProperties(
                new BotProperties.DiscordProperties("token", 123456789L, "dev-guild", ""),
                new BotProperties.MinioProperties("http://localhost:9000", "access", "secret", "bucket", "https://media.fashionscape.org")
        );
        command = new OutfitSlashCommand(outfitService, new PermissionService(staticPermissionRepository, properties));

        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("1001");
        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(slashEvent.replyEmbeds(any(MessageEmbed.class))).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(anyBoolean())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setActionRow(any(net.dv8tion.jda.api.interactions.components.ItemComponent[].class))).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setComponents(any(net.dv8tion.jda.api.interactions.components.LayoutComponent[].class))).thenReturn(replyCallbackAction);
    }

    @Test
    void testCommandDataStructure() {
        assertEquals("outfit", command.getName());
        assertNotNull(command.getDescription());

        SlashCommandData data = command.buildCommandData();
        assertEquals("outfit", data.getName());
        assertEquals(5, data.getSubcommands().size());
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("random")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("info")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("search")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("delete")));
        assertTrue(data.getSubcommands().stream().anyMatch(s -> s.getName().equals("restore")));
    }

    @Test
    void testExecuteRandomSuccess() {
        when(slashEvent.getSubcommandName()).thenReturn("random");
        OutfitEntity outfit = new OutfitEntity("outfit-1", "https://img.png", "1001", "osrs", "cool outfit", "Player1", Instant.now(), Instant.now(), false, false, 3, null, "server-1", "channel-1");
        when(outfitService.getRandomOutfit(null, null)).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(slashEvent).replyEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();
        assertEquals("Outfit by Player1", embed.getTitle());
        verify(replyCallbackAction).queue();
    }

    @Test
    void testExecuteInfoSuccess() {
        when(slashEvent.getSubcommandName()).thenReturn("info");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("outfit-123");

        OutfitEntity outfit = new OutfitEntity("outfit-123", "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 7, null, "server-1", "channel-1");
        when(outfitService.findById("outfit-123")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(slashEvent).replyEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();
        assertEquals("Outfit Details: outfit-123", embed.getTitle());
    }

    @Test
    void testExecuteDeleteBySubmitter() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("outfit-to-delete");

        OutfitEntity outfit = new OutfitEntity("outfit-to-delete", "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("outfit-to-delete")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(outfitService).delete("outfit-to-delete");
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testExecuteDeleteUnauthorized() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("outfit-not-mine");
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(any(Permission.class))).thenReturn(false);

        OutfitEntity outfit = new OutfitEntity("outfit-not-mine", "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("outfit-not-mine")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(outfitService, never()).delete("outfit-not-mine");
        verify(slashEvent).reply(contains("You do not have permission"));
    }

    @Test
    void testExecuteDeleteByAdminOfAnotherServerRefused() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("outfit-elsewhere");
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(any(Permission.class))).thenReturn(true);
        when(mockMember.getGuild()).thenReturn(mockGuild);
        when(mockGuild.getId()).thenReturn("server-2");

        OutfitEntity outfit = new OutfitEntity("outfit-elsewhere", "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("outfit-elsewhere")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(outfitService, never()).delete("outfit-elsewhere");
        verify(slashEvent).reply(contains("You do not have permission"));
    }

    @Test
    void testExecuteRestoreByServerAdminRefused() {
        when(slashEvent.getSubcommandName()).thenReturn("restore");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("outfit-del");
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(any(Permission.class))).thenReturn(true);

        command.execute(slashEvent);

        verify(outfitService, never()).restore(anyString());
        verify(slashEvent).reply(contains("staff member"));
    }

    @Test
    void testExecuteRestoreByStaff() {
        when(slashEvent.getSubcommandName()).thenReturn("restore");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("outfit-del");
        when(mockUser.getId()).thenReturn("123456789"); // bot owner id

        OutfitEntity outfit = new OutfitEntity("outfit-del", "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), true, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("outfit-del")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(outfitService).restore("outfit-del");
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testSearchAndPaginationButtons() {
        when(slashEvent.getSubcommandName()).thenReturn("search");
        OutfitEntity outfit = new OutfitEntity("o-1", "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 1, null, "server-1", "channel-1");
        when(outfitService.search(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(outfit)));

        command.execute(slashEvent);

        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));

        // Test button handler
        assertTrue(command.canHandle("outfit:page:next:2:osrs:1001"));
        assertFalse(command.canHandle("gallery:upvote:123"));

        when(buttonEvent.getComponentId()).thenReturn("outfit:page:next:2:osrs:1001");
        when(buttonEvent.editMessageEmbeds(any(MessageEmbed.class))).thenReturn(messageEditCallbackAction);
        when(messageEditCallbackAction.setComponents(any(net.dv8tion.jda.api.interactions.components.LayoutComponent[].class))).thenReturn(messageEditCallbackAction);

        command.handle(buttonEvent);

        verify(buttonEvent).editMessageEmbeds(any(MessageEmbed.class));
    }
}
