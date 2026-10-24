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
import java.util.UUID;

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
    private net.dv8tion.jda.api.interactions.InteractionHook interactionHook;

    @Mock
    private net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction<net.dv8tion.jda.api.entities.Message> webhookAction;

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
                new BotProperties.SeaweedProperties("http://localhost:8333", "access", "secret", "bucket", "https://media.fashionscape.org")
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
        OutfitEntity outfit = new OutfitEntity(UUID.fromString("b024659c-6488-5fda-adde-f63d03e96f27"), "https://img.png", "1001", "osrs", "cool outfit", "Player1", Instant.now(), Instant.now(), false, false, 3, null, "server-1", "channel-1");
        when(outfitService.getRandomOutfit(null, null)).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(slashEvent).replyEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();
        assertEquals("Outfit by Player1", embed.getTitle());
        verify(outfitService).recordShown(UUID.fromString("b024659c-6488-5fda-adde-f63d03e96f27"));
        verify(replyCallbackAction).queue();
    }

    @Test
    void testExecuteInfoSuccess() {
        when(slashEvent.getSubcommandName()).thenReturn("info");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("8e6efe94-c34b-5afd-aec1-252df5c4a5de");

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("8e6efe94-c34b-5afd-aec1-252df5c4a5de"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 7, null, "server-1", "channel-1");
        when(outfitService.findById("8e6efe94-c34b-5afd-aec1-252df5c4a5de")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(slashEvent).replyEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();
        assertEquals("Outfit Details: 8e6efe94-c34b-5afd-aec1-252df5c4a5de", embed.getTitle());
        assertTrue(embed.getFields().stream().anyMatch(f -> "Upvotes".equals(f.getName()) && "0".equals(f.getValue())));
        assertTrue(embed.getFields().stream().anyMatch(f -> "Times shown".equals(f.getName()) && "7".equals(f.getValue())));
        verify(outfitService, never()).recordShown(any());
    }

    @Test
    void testExecuteDeleteBySubmitter() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("df9499f4-413f-59f8-848b-519790a58953");

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("df9499f4-413f-59f8-848b-519790a58953"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("df9499f4-413f-59f8-848b-519790a58953")).thenReturn(Optional.of(outfit));

        when(outfitService.delete(eq("df9499f4-413f-59f8-848b-519790a58953"), eq("1001"), any(), any())).thenReturn(true);

        command.execute(slashEvent);

        verify(outfitService).delete(eq("df9499f4-413f-59f8-848b-519790a58953"), eq("1001"), any(), any());
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testExecuteDeleteImageMoveFails() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("df9499f4-413f-59f8-848b-519790a58953");

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("df9499f4-413f-59f8-848b-519790a58953"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("df9499f4-413f-59f8-848b-519790a58953")).thenReturn(Optional.of(outfit));
        when(outfitService.delete(eq("df9499f4-413f-59f8-848b-519790a58953"), eq("1001"), any(), any()))
                .thenThrow(new OutfitService.OutfitImageException("boom", new RuntimeException()));

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("Nothing was changed"));
        verify(slashEvent, never()).replyEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testExecuteDeleteAlreadyDeletedLooksMissingToNonStaff() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("05258b6b-8f8e-575f-82de-95e21856f70f");

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("05258b6b-8f8e-575f-82de-95e21856f70f"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), true, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("05258b6b-8f8e-575f-82de-95e21856f70f")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(slashEvent).reply(contains("not found"));
        verify(outfitService, never()).delete(any(), any(), any(), any());
    }

    @Test
    void testInfoOnDeletedOutfitIsNotFoundForNonStaff() {
        when(slashEvent.getSubcommandName()).thenReturn("info");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("05258b6b-8f8e-575f-82de-95e21856f70f");

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("05258b6b-8f8e-575f-82de-95e21856f70f"), "https://media.fashionscape.org/outfits/05258b6b-8f8e-575f-82de-95e21856f70f.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), true, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("05258b6b-8f8e-575f-82de-95e21856f70f")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(slashEvent).reply("Outfit `05258b6b-8f8e-575f-82de-95e21856f70f` not found.");
        verify(slashEvent, never()).replyEmbeds(any(MessageEmbed.class));
        verify(outfitService, never()).getDeletedImage(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testInfoOnDeletedOutfitForStaffIsEphemeralWithAttachedImage() {
        when(slashEvent.getSubcommandName()).thenReturn("info");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("05258b6b-8f8e-575f-82de-95e21856f70f");
        when(mockUser.getId()).thenReturn("123456789"); // bot owner id
        when(slashEvent.deferReply(true)).thenReturn(replyCallbackAction);
        when(slashEvent.getHook()).thenReturn(interactionHook);
        when(interactionHook.sendMessageEmbeds(any(MessageEmbed.class))).thenReturn(webhookAction);
        when(webhookAction.addFiles(any(net.dv8tion.jda.api.utils.FileUpload[].class))).thenReturn(webhookAction);

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("05258b6b-8f8e-575f-82de-95e21856f70f"), "https://media.fashionscape.org/outfits/05258b6b-8f8e-575f-82de-95e21856f70f.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), true, false, 0, null, "server-1", "channel-1");
        outfit.setDeletedBy("1001");
        outfit.setDeletedAt(Instant.now());
        when(outfitService.findById("05258b6b-8f8e-575f-82de-95e21856f70f")).thenReturn(Optional.of(outfit));
        when(outfitService.getDeletedImage(outfit))
                .thenReturn(Optional.of(new OutfitService.StoredImage("05258b6b-8f8e-575f-82de-95e21856f70f.png", new byte[]{1})));

        command.execute(slashEvent);

        verify(slashEvent).deferReply(true);
        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(interactionHook).sendMessageEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();
        assertEquals("attachment://05258b6b-8f8e-575f-82de-95e21856f70f.png", embed.getImage().getUrl());
        assertTrue(embed.getFields().stream().anyMatch(f -> "Deleted by".equals(f.getName()) && "<@1001>".equals(f.getValue())));
        verify(webhookAction).addFiles(any(net.dv8tion.jda.api.utils.FileUpload[].class));
        verify(webhookAction).queue();
        verify(slashEvent, never()).replyEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testExecuteDeleteUnauthorized() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("e05b2bb6-eebd-54be-a9c1-7f114b8199a5");
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(any(Permission.class))).thenReturn(false);

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("e05b2bb6-eebd-54be-a9c1-7f114b8199a5"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("e05b2bb6-eebd-54be-a9c1-7f114b8199a5")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(outfitService, never()).delete(any(), any(), any(), any());
        verify(slashEvent).reply(contains("You do not have permission"));
    }

    @Test
    void testExecuteDeleteByAdminOfAnotherServerRefused() {
        when(slashEvent.getSubcommandName()).thenReturn("delete");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("febd3ab8-89fd-5c4d-b286-e2041422bab2");
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(any(Permission.class))).thenReturn(true);
        when(mockMember.getGuild()).thenReturn(mockGuild);
        when(mockGuild.getId()).thenReturn("server-2");

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("febd3ab8-89fd-5c4d-b286-e2041422bab2"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("febd3ab8-89fd-5c4d-b286-e2041422bab2")).thenReturn(Optional.of(outfit));

        command.execute(slashEvent);

        verify(outfitService, never()).delete(any(), any(), any(), any());
        verify(slashEvent).reply(contains("You do not have permission"));
    }

    @Test
    void testExecuteRestoreByServerAdminRefused() {
        when(slashEvent.getSubcommandName()).thenReturn("restore");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("a4f42f88-4814-519a-b91f-eb89d216d660");
        when(mockUser.getId()).thenReturn("9999");
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(mockMember.hasPermission(any(Permission.class))).thenReturn(true);

        command.execute(slashEvent);

        verify(outfitService, never()).restore(any(), any(), any(), any());
        verify(slashEvent).reply(contains("staff member"));
    }

    @Test
    void testExecuteRestoreByStaff() {
        when(slashEvent.getSubcommandName()).thenReturn("restore");
        when(slashEvent.getOption(eq("id"), any())).thenReturn("a4f42f88-4814-519a-b91f-eb89d216d660");
        when(mockUser.getId()).thenReturn("123456789"); // bot owner id

        OutfitEntity outfit = new OutfitEntity(UUID.fromString("a4f42f88-4814-519a-b91f-eb89d216d660"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), true, false, 0, null, "server-1", "channel-1");
        when(outfitService.findById("a4f42f88-4814-519a-b91f-eb89d216d660")).thenReturn(Optional.of(outfit));

        when(outfitService.restore(eq("a4f42f88-4814-519a-b91f-eb89d216d660"), eq("123456789"), any(), any())).thenReturn(true);

        command.execute(slashEvent);

        verify(outfitService).restore(eq("a4f42f88-4814-519a-b91f-eb89d216d660"), eq("123456789"), any(), any());
        verify(slashEvent).replyEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testSearchAndPaginationButtons() {
        when(slashEvent.getSubcommandName()).thenReturn("search");
        OutfitEntity outfit = new OutfitEntity(UUID.fromString("32ad0af8-600d-573c-97e4-9e1c34755075"), "https://img.png", "1001", "osrs", "my outfit", "Player1", Instant.now(), Instant.now(), false, false, 1, null, "server-1", "channel-1");
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
