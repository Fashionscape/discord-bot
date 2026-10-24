package dev.salmonllama.fsbot.discord.listeners;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.GalleryChannelRepository;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.database.repositories.StaticPermissionRepository;
import dev.salmonllama.fsbot.services.BlocklistService;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.OutfitService;
import dev.salmonllama.fsbot.services.PermissionService;
import dev.salmonllama.fsbot.storage.MinioStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GalleryEventListenerTest {

    @Mock
    private GalleryChannelRepository galleryChannelRepository;

    @Mock
    private OutfitRepository outfitRepository;

    @Mock
    private OutfitService outfitService;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private BlocklistService blocklistService;

    @Mock
    private StaticPermissionRepository staticPermissionRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MessageReceivedEvent messageEvent;

    @Mock
    private ButtonInteractionEvent buttonEvent;

    @Mock
    private Message message;

    @Mock
    private User author;

    @Mock
    private Guild guild;

    @Mock
    private MessageChannelUnion channel;

    @Mock
    private Member member;

    @Mock
    private InteractionHook interactionHook;

    @Mock
    private MessageCreateAction messageCreateAction;

    @Mock
    private MessageEditCallbackAction messageEditCallbackAction;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    private GalleryEventListener listener;

    @BeforeEach
    void setUp() {
        BotProperties properties = new BotProperties(
                new BotProperties.DiscordProperties("token", 123456789L, "", "home-guild"),
                new BotProperties.SeaweedProperties("http://localhost:8333", "access", "secret", "bucket", "https://media.fashionscape.org")
        );
        listener = new GalleryEventListener(
                galleryChannelRepository,
                outfitRepository,
                outfitService,
                minioStorageService,
                blocklistService,
                new PermissionService(staticPermissionRepository, properties),
                auditService
        );
    }

    @Test
    void testCanHandle() {
        assertTrue(listener.canHandle("gallery:upvote:123"));
        assertTrue(listener.canHandle("gallery:delete:456"));
        assertFalse(listener.canHandle("other:button:789"));
        assertFalse(listener.canHandle(null));
    }

    @Test
    void testIgnoreBotMessage() {
        when(messageEvent.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(true);

        listener.onMessageReceived(messageEvent);

        verifyNoInteractions(galleryChannelRepository, minioStorageService, outfitRepository);
    }

    @Test
    void testIgnoreNonGuildMessage() {
        when(messageEvent.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(messageEvent.isWebhookMessage()).thenReturn(false);
        when(messageEvent.isFromGuild()).thenReturn(false);

        listener.onMessageReceived(messageEvent);

        verifyNoInteractions(galleryChannelRepository, minioStorageService, outfitRepository);
    }

    @Test
    void testIgnoreBlockedUserOrServer() {
        when(messageEvent.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(messageEvent.isWebhookMessage()).thenReturn(false);
        when(messageEvent.isFromGuild()).thenReturn(true);
        when(author.getId()).thenReturn("bad-user");
        when(messageEvent.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("srv-1");
        when(messageEvent.getChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn("chan-1");

        when(blocklistService.isBlocked("bad-user", "srv-1")).thenReturn(true);

        listener.onMessageReceived(messageEvent);

        verifyNoInteractions(galleryChannelRepository, minioStorageService, outfitRepository);
    }

    @Test
    void testIgnoreNonGalleryChannel() {
        when(messageEvent.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(messageEvent.isWebhookMessage()).thenReturn(false);
        when(messageEvent.isFromGuild()).thenReturn(true);
        when(author.getId()).thenReturn("user-1");
        when(messageEvent.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("srv-1");
        when(messageEvent.getChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn("chan-regular");

        when(blocklistService.isBlocked("user-1", "srv-1")).thenReturn(false);
        when(galleryChannelRepository.findByChannelIdAndEnabledTrue("chan-regular")).thenReturn(Optional.empty());

        listener.onMessageReceived(messageEvent);

        verifyNoInteractions(minioStorageService, outfitRepository);
    }

    @Test
    void testSuccessfulImageUploadAndPost() throws Exception {
        when(messageEvent.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(messageEvent.isWebhookMessage()).thenReturn(false);
        when(messageEvent.isFromGuild()).thenReturn(true);
        when(author.getId()).thenReturn("user-1");
        when(author.getName()).thenReturn("TestUser");
        when(author.getAsMention()).thenReturn("<@user-1>");
        when(messageEvent.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("srv-1");
        when(messageEvent.getChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn("chan-gallery");
        when(messageEvent.getMessage()).thenReturn(message);
        when(message.getContentRaw()).thenReturn("My cool outfit");

        GalleryChannelEntity galleryEntity = new GalleryChannelEntity("chan-gallery", "srv-1", "❤️", "osrs", true);
        when(blocklistService.isBlocked("user-1", "srv-1")).thenReturn(false);
        when(galleryChannelRepository.findByChannelIdAndEnabledTrue("chan-gallery")).thenReturn(Optional.of(galleryEntity));

        Message.Attachment attachment = mock(Message.Attachment.class);
        when(attachment.isImage()).thenReturn(true);
        when(attachment.getUrl()).thenReturn("https://cdn.discordapp.com/attachments/1/2/pic.png");
        when(message.getAttachments()).thenReturn(List.of(attachment));

        UploadResult uploadResult = new UploadResult("outfits/id.png", "https://media.fashionscape.org/outfits/id.png", "bucket", 1024, "image/png");
        when(minioStorageService.uploadOutfitFromUrl(anyString(), eq("https://cdn.discordapp.com/attachments/1/2/pic.png")))
                .thenReturn(uploadResult);

        when(channel.sendMessageEmbeds(any(MessageEmbed.class))).thenReturn(messageCreateAction);
        when(messageCreateAction.setActionRow(any(), any())).thenReturn(messageCreateAction);

        listener.onMessageReceived(messageEvent);

        ArgumentCaptor<OutfitEntity> outfitCaptor = ArgumentCaptor.forClass(OutfitEntity.class);
        verify(outfitRepository).save(outfitCaptor.capture());

        OutfitEntity savedOutfit = outfitCaptor.getValue();
        assertEquals("user-1", savedOutfit.getSubmitter());
        assertEquals("osrs", savedOutfit.getTag());
        assertEquals("My cool outfit", savedOutfit.getMeta());
        assertEquals("https://media.fashionscape.org/outfits/id.png", savedOutfit.getLink());
        assertEquals("srv-1", savedOutfit.getServerId());
        assertEquals("chan-gallery", savedOutfit.getChannelId());
        verify(auditService).record(any(), eq(AuditService.Action.OUTFIT_ADDED), eq("user-1"), eq("srv-1"),
                eq(AuditService.TARGET_OUTFIT), eq(savedOutfit.getId()), any());
        verify(messageCreateAction).queue(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleUpvote() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:upvote:outfit-99");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("voter-1");
        when(buttonEvent.getHook()).thenReturn(interactionHook);
        when(buttonEvent.getMessage()).thenReturn(message);
        when(message.getButtons()).thenReturn(List.of(
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("gallery:upvote:outfit-99", "Upvote (3)"),
                net.dv8tion.jda.api.interactions.components.buttons.Button.danger("gallery:delete:outfit-99", "Delete")
        ));
        when(outfitService.toggleVote("outfit-99", "voter-1"))
                .thenReturn(Optional.of(new OutfitService.VoteResult(true, 4)));
        when(buttonEvent.editComponents(any(net.dv8tion.jda.api.interactions.components.ActionRow.class)))
                .thenReturn(messageEditCallbackAction);

        doAnswer(invocation -> {
            Consumer<Void> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(messageEditCallbackAction).queue(any(Consumer.class));

        WebhookMessageCreateAction webhookAction = mock(WebhookMessageCreateAction.class);
        when(interactionHook.sendMessage(anyString())).thenReturn(webhookAction);
        when(webhookAction.setEphemeral(true)).thenReturn(webhookAction);

        listener.handle(buttonEvent);

        ArgumentCaptor<net.dv8tion.jda.api.interactions.components.ActionRow> rowCaptor =
                ArgumentCaptor.forClass(net.dv8tion.jda.api.interactions.components.ActionRow.class);
        verify(buttonEvent).editComponents(rowCaptor.capture());
        List<net.dv8tion.jda.api.interactions.components.buttons.Button> buttons = rowCaptor.getValue().getButtons();
        assertEquals(2, buttons.size());
        assertEquals("Upvote (4)", buttons.get(0).getLabel());
        assertEquals("Delete", buttons.get(1).getLabel());
        verify(interactionHook).sendMessage(contains("Vote added"));
        verify(webhookAction).queue();
        verify(outfitRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleUpvoteToggleOff() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:upvote:outfit-99");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("voter-1");
        when(buttonEvent.getHook()).thenReturn(interactionHook);
        when(buttonEvent.getMessage()).thenReturn(message);
        when(message.getButtons()).thenReturn(List.of(
                net.dv8tion.jda.api.interactions.components.buttons.Button.primary("gallery:upvote:outfit-99", "Upvote (4)")
        ));
        when(outfitService.toggleVote("outfit-99", "voter-1"))
                .thenReturn(Optional.of(new OutfitService.VoteResult(false, 3)));
        when(buttonEvent.editComponents(any(net.dv8tion.jda.api.interactions.components.ActionRow.class)))
                .thenReturn(messageEditCallbackAction);

        doAnswer(invocation -> {
            Consumer<Void> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(messageEditCallbackAction).queue(any(Consumer.class));

        WebhookMessageCreateAction webhookAction = mock(WebhookMessageCreateAction.class);
        when(interactionHook.sendMessage(anyString())).thenReturn(webhookAction);
        when(webhookAction.setEphemeral(true)).thenReturn(webhookAction);

        listener.handle(buttonEvent);

        ArgumentCaptor<net.dv8tion.jda.api.interactions.components.ActionRow> rowCaptor =
                ArgumentCaptor.forClass(net.dv8tion.jda.api.interactions.components.ActionRow.class);
        verify(buttonEvent).editComponents(rowCaptor.capture());
        assertEquals(1, rowCaptor.getValue().getButtons().size());
        assertEquals("Upvote (3)", rowCaptor.getValue().getButtons().get(0).getLabel());
        verify(interactionHook).sendMessage(contains("Vote removed"));
    }

    @Test
    void testHandleUpvoteOnDeletedOutfit() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:upvote:outfit-gone");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("voter-1");
        when(outfitService.toggleVote("outfit-gone", "voter-1")).thenReturn(Optional.empty());
        when(buttonEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);

        listener.handle(buttonEvent);

        verify(buttonEvent).reply(contains("could not be found"));
        verify(buttonEvent, never()).editComponents(any(net.dv8tion.jda.api.interactions.components.ActionRow.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleDeleteBySubmitter() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:delete:outfit-99");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("user-submitter");
        when(author.getAsMention()).thenReturn("<@user-submitter>");
        when(buttonEvent.getHook()).thenReturn(interactionHook);

        OutfitEntity outfit = new OutfitEntity(
                "outfit-99", "https://media.fashionscape.org/1.png", "user-submitter", "tag", null,
                "Player", Instant.now(), Instant.now(), false, false, 0, null,
                "server-1",
                "channel-1"
        );
        when(outfitRepository.findByIdAndDeletedFalse("outfit-99")).thenReturn(Optional.of(outfit));
        when(buttonEvent.editMessageEmbeds(any(MessageEmbed.class))).thenReturn(messageEditCallbackAction);
        when(messageEditCallbackAction.setComponents()).thenReturn(messageEditCallbackAction);

        doAnswer(invocation -> {
            Consumer<Void> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(messageEditCallbackAction).queue(any(Consumer.class));

        WebhookMessageCreateAction webhookAction = mock(WebhookMessageCreateAction.class);
        when(interactionHook.sendMessage(anyString())).thenReturn(webhookAction);
        when(webhookAction.setEphemeral(true)).thenReturn(webhookAction);

        when(outfitService.delete(eq("outfit-99"), eq("user-submitter"), any(), any())).thenReturn(true);

        listener.handle(buttonEvent);

        // Deletion goes through OutfitService, which quarantines the image and audits
        verify(outfitService).delete(eq("outfit-99"), eq("user-submitter"), any(), any());
        verify(outfitRepository, never()).save(any());
        verify(webhookAction).queue();
    }

    @Test
    void testHandleDeleteImageMoveFails() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:delete:outfit-99");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("user-submitter");

        OutfitEntity outfit = new OutfitEntity(
                "outfit-99", "https://media.fashionscape.org/outfits/outfit-99.png", "user-submitter", "tag", null,
                "Player", Instant.now(), Instant.now(), false, false, 0, null,
                "server-1",
                "channel-1"
        );
        when(outfitRepository.findByIdAndDeletedFalse("outfit-99")).thenReturn(Optional.of(outfit));
        when(outfitService.delete(eq("outfit-99"), eq("user-submitter"), any(), any()))
                .thenThrow(new OutfitService.OutfitImageException("boom", new RuntimeException()));
        when(buttonEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);

        listener.handle(buttonEvent);

        verify(buttonEvent).reply(contains("Nothing was changed"));
        verify(buttonEvent, never()).editMessageEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testHandleDeleteForbidden() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:delete:outfit-99");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("stranger");
        when(buttonEvent.getMember()).thenReturn(member);
        when(member.hasPermission(Permission.ADMINISTRATOR)).thenReturn(false);
        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(false);
        when(member.hasPermission(Permission.MESSAGE_MANAGE)).thenReturn(false);

        OutfitEntity outfit = new OutfitEntity(
                "outfit-99", "https://media.fashionscape.org/1.png", "author-owner", "tag", null,
                "Player", Instant.now(), Instant.now(), false, false, 0, null,
                "server-1",
                "channel-1"
        );
        when(outfitRepository.findByIdAndDeletedFalse("outfit-99")).thenReturn(Optional.of(outfit));
        when(buttonEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);

        listener.handle(buttonEvent);

        verifyNoInteractions(outfitService);
        verify(buttonEvent).reply(contains("do not have permission"));
        verifyNoInteractions(auditService);
    }

    @Test
    void testHandleDeleteByModeratorOfAnotherServerForbidden() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:delete:outfit-99");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("other-server-mod");
        when(buttonEvent.getMember()).thenReturn(member);
        when(member.hasPermission(Permission.ADMINISTRATOR)).thenReturn(true);
        when(member.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("server-2");

        OutfitEntity outfit = new OutfitEntity(
                "outfit-99", "https://media.fashionscape.org/1.png", "author-owner", "tag", null,
                "Player", Instant.now(), Instant.now(), false, false, 0, null,
                "server-1",
                "channel-1"
        );
        when(outfitRepository.findByIdAndDeletedFalse("outfit-99")).thenReturn(Optional.of(outfit));
        when(buttonEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);

        listener.handle(buttonEvent);

        verifyNoInteractions(outfitService);
        verify(buttonEvent).reply(contains("do not have permission"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleDeleteByModeratorOfSameServer() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:delete:outfit-99");
        when(buttonEvent.getUser()).thenReturn(author);
        when(author.getId()).thenReturn("home-mod");
        when(author.getAsMention()).thenReturn("<@home-mod>");
        when(buttonEvent.getMember()).thenReturn(member);
        when(member.hasPermission(Permission.ADMINISTRATOR)).thenReturn(false);
        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(false);
        when(member.hasPermission(Permission.MESSAGE_MANAGE)).thenReturn(true);
        when(member.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("server-1");
        when(buttonEvent.getHook()).thenReturn(interactionHook);

        OutfitEntity outfit = new OutfitEntity(
                "outfit-99", "https://media.fashionscape.org/1.png", "author-owner", "tag", null,
                "Player", Instant.now(), Instant.now(), false, false, 0, null,
                "server-1",
                "channel-1"
        );
        when(outfitRepository.findByIdAndDeletedFalse("outfit-99")).thenReturn(Optional.of(outfit));
        when(buttonEvent.editMessageEmbeds(any(MessageEmbed.class))).thenReturn(messageEditCallbackAction);
        when(messageEditCallbackAction.setComponents()).thenReturn(messageEditCallbackAction);

        doAnswer(invocation -> {
            Consumer<Void> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(messageEditCallbackAction).queue(any(Consumer.class));

        WebhookMessageCreateAction webhookAction = mock(WebhookMessageCreateAction.class);
        when(interactionHook.sendMessage(anyString())).thenReturn(webhookAction);
        when(webhookAction.setEphemeral(true)).thenReturn(webhookAction);

        when(outfitService.delete(eq("outfit-99"), eq("home-mod"), any(), any())).thenReturn(true);

        listener.handle(buttonEvent);

        verify(outfitService).delete(eq("outfit-99"), eq("home-mod"), any(), any());
        verify(webhookAction).queue();
    }
}
