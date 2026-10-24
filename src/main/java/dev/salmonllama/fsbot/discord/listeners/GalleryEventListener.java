package dev.salmonllama.fsbot.discord.listeners;

import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.GalleryChannelRepository;
import dev.salmonllama.fsbot.discord.components.ButtonHandler;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.BlocklistService;
import dev.salmonllama.fsbot.services.OutfitService;
import dev.salmonllama.fsbot.services.PermissionService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class GalleryEventListener extends ListenerAdapter implements ButtonHandler {

    private static final Logger logger = LoggerFactory.getLogger(GalleryEventListener.class);

    private static final String BUTTON_PREFIX = "gallery:";
    private static final String UPVOTE_PREFIX = "gallery:upvote:";
    private static final String DELETE_PREFIX = "gallery:delete:";

    private final GalleryChannelRepository galleryChannelRepository;
    private final OutfitService outfitService;
    private final BlocklistService blocklistService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public GalleryEventListener(
            GalleryChannelRepository galleryChannelRepository,
            OutfitService outfitService,
            BlocklistService blocklistService,
            PermissionService permissionService,
            AuditService auditService
    ) {
        this.galleryChannelRepository = galleryChannelRepository;
        this.outfitService = outfitService;
        this.blocklistService = blocklistService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Override
    public boolean canHandle(String buttonId) {
        return buttonId != null && buttonId.startsWith(BUTTON_PREFIX);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.isWebhookMessage()) {
            return;
        }

        if (!event.isFromGuild()) {
            return;
        }

        String userId = event.getAuthor().getId();
        String serverId = event.getGuild().getId();
        String channelId = event.getChannel().getId();

        if (blocklistService.isBlocked(userId, serverId)) {
            logger.warn("Ignoring gallery message from blocked user {} or server {}", userId, serverId);
            return;
        }

        Optional<GalleryChannelEntity> galleryOpt = galleryChannelRepository.findByChannelIdAndEnabledTrue(channelId);
        if (galleryOpt.isEmpty()) {
            return;
        }

        GalleryChannelEntity gallery = galleryOpt.get();
        List<Message.Attachment> imageAttachments = event.getMessage().getAttachments().stream()
                .filter(Message.Attachment::isImage)
                .toList();

        if (imageAttachments.isEmpty()) {
            return;
        }

        logger.info("Processing {} gallery image(s) from user {} in channel {}", imageAttachments.size(), userId, channelId);

        for (Message.Attachment attachment : imageAttachments) {
            try {
                String sourceUrl = attachment.getUrl();
                String tag = gallery.getTag() != null && !gallery.getTag().isBlank() ? gallery.getTag() : "general";
                String meta = event.getMessage().getContentRaw();
                String discordName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();

                OutfitEntity outfit = outfitService.submit(new OutfitEntity(
                        null,
                        sourceUrl,
                        userId,
                        tag,
                        meta,
                        discordName,
                        Instant.now(),
                        Instant.now(),
                        false,
                        false,
                        0,
                        null,
                        serverId,
                        channelId
                ), sourceUrl);
                String outfitId = outfit.getId().toString();

                logger.info("Saved outfit {} submitted by {}", outfitId, userId);
                auditService.record(event.getJDA(), AuditService.Action.OUTFIT_ADDED, userId, serverId,
                        AuditService.TARGET_OUTFIT, outfitId, "Submitted in <#" + channelId + "> with tag `" + tag + "`");

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("New Outfit Submission!")
                        .setImage(outfit.getLink())
                        .setColor(new Color(0x3B88C3))
                        .addField("Submitter", event.getAuthor().getAsMention(), true)
                        .addField("Tag", tag, true)
                        .addField("Outfit ID", "`" + outfitId + "`", true)
                        .setFooter("FashionScape Bot • Vote with the buttons below!");

                if (meta != null && !meta.isBlank()) {
                    embed.setDescription(meta);
                }

                Button upvoteButton = Button.primary(UPVOTE_PREFIX + outfitId, "Upvote (0)");
                Button deleteButton = Button.danger(DELETE_PREFIX + outfitId, "Delete");

                event.getChannel().sendMessageEmbeds(embed.build())
                        .setActionRow(upvoteButton, deleteButton)
                        .queue(
                                success -> logger.debug("Posted outfit showcase for {}", outfitId),
                                error -> logger.error("Failed to post outfit showcase for {}", outfitId, error)
                        );

            } catch (Exception e) {
                logger.error("Failed to process gallery upload for user {} in channel {}", userId, channelId, e);
            }
        }
    }

    @Override
    public void handle(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId.startsWith(UPVOTE_PREFIX)) {
            handleUpvote(event, buttonId.substring(UPVOTE_PREFIX.length()));
        } else if (buttonId.startsWith(DELETE_PREFIX)) {
            handleDelete(event, buttonId.substring(DELETE_PREFIX.length()));
        }
    }

    private void handleUpvote(ButtonInteractionEvent event, String outfitId) {
        Optional<OutfitService.VoteResult> result = outfitService.toggleVote(outfitId, event.getUser().getId());

        if (result.isEmpty()) {
            event.reply("This outfit could not be found or has been removed.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OutfitService.VoteResult vote = result.get();
        String label = "Upvote (" + vote.voteCount() + ")";
        List<Button> buttons = event.getMessage().getButtons().stream()
                .map(button -> (UPVOTE_PREFIX + outfitId).equals(button.getId()) ? button.withLabel(label) : button)
                .toList();

        event.editComponents(ActionRow.of(buttons)).queue(
                success -> event.getHook().sendMessage(vote.added()
                                ? "Vote added. Outfit `" + outfitId + "` now has " + vote.voteCount() + " votes."
                                : "Vote removed. Outfit `" + outfitId + "` now has " + vote.voteCount() + " votes.")
                        .setEphemeral(true)
                        .queue()
        );
    }

    private void handleDelete(ButtonInteractionEvent event, String outfitId) {
        Optional<OutfitEntity> outfitOpt = outfitService.findActiveById(outfitId);

        if (outfitOpt.isEmpty()) {
            event.reply("This outfit could not be found or has already been removed.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OutfitEntity outfit = outfitOpt.get();
        String userId = event.getUser().getId();
        Member member = event.getMember();

        if (!permissionService.canModerateOutfit(userId, member, outfit)) {
            event.reply("You do not have permission to delete this outfit. Only the submitter, bot staff, or moderators of the server it was submitted in can delete it.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        try {
            if (!outfitService.delete(outfitId, userId, event.getGuild() != null ? event.getGuild().getId() : null, event.getJDA())) {
                event.reply("This outfit could not be found or has already been removed.")
                        .setEphemeral(true)
                        .queue();
                return;
            }
        } catch (OutfitService.OutfitImageException e) {
            logger.error("Failed to delete outfit {}", outfitId, e);
            event.reply("This outfit could not be deleted because its image couldn't be moved. Nothing was changed; please try again.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        EmbedBuilder deletedEmbed = new EmbedBuilder()
                .setTitle("Outfit Removed")
                .setDescription("Outfit `" + outfitId + "` has been deleted by " + event.getUser().getAsMention() + ".")
                .setColor(Color.DARK_GRAY)
                .setTimestamp(Instant.now());

        event.editMessageEmbeds(deletedEmbed.build())
                .setComponents() // clear all buttons
                .queue(
                        success -> event.getHook().sendMessage("Outfit `" + outfitId + "` was deleted successfully.")
                                .setEphemeral(true)
                                .queue()
                );
    }
}
