package dev.salmonllama.fsbot.discord.listeners;

import dev.salmonllama.fsbot.database.entities.GalleryChannelEntity;
import dev.salmonllama.fsbot.database.entities.OutfitEntity;
import dev.salmonllama.fsbot.database.repositories.GalleryChannelRepository;
import dev.salmonllama.fsbot.database.repositories.OutfitRepository;
import dev.salmonllama.fsbot.discord.components.ButtonHandler;
import dev.salmonllama.fsbot.services.AuditService;
import dev.salmonllama.fsbot.services.BlocklistService;
import dev.salmonllama.fsbot.services.IdService;
import dev.salmonllama.fsbot.services.PermissionService;
import dev.salmonllama.fsbot.storage.MinioStorageService;
import dev.salmonllama.fsbot.storage.UploadResult;
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
    private final OutfitRepository outfitRepository;
    private final MinioStorageService minioStorageService;
    private final BlocklistService blocklistService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public GalleryEventListener(
            GalleryChannelRepository galleryChannelRepository,
            OutfitRepository outfitRepository,
            MinioStorageService minioStorageService,
            BlocklistService blocklistService,
            PermissionService permissionService,
            AuditService auditService
    ) {
        this.galleryChannelRepository = galleryChannelRepository;
        this.outfitRepository = outfitRepository;
        this.minioStorageService = minioStorageService;
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
                String outfitId = IdService.generateShort();
                String sourceUrl = attachment.getUrl();

                UploadResult uploadResult = minioStorageService.uploadOutfitFromUrl(outfitId, sourceUrl);

                String tag = gallery.getTag() != null && !gallery.getTag().isBlank() ? gallery.getTag() : "general";
                String meta = event.getMessage().getContentRaw();
                String discordName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();

                OutfitEntity outfit = new OutfitEntity(
                        outfitId,
                        uploadResult.publicUrl(),
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
                );

                outfitRepository.save(outfit);
                logger.info("Saved outfit {} submitted by {}", outfitId, userId);
                auditService.record(event.getJDA(), AuditService.Action.OUTFIT_ADDED, userId, serverId,
                        AuditService.TARGET_OUTFIT, outfitId, "Submitted in <#" + channelId + "> with tag `" + tag + "`");

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("New Outfit Submission!")
                        .setImage(uploadResult.publicUrl())
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
        Optional<OutfitEntity> outfitOpt = outfitRepository.findByIdAndDeletedFalse(outfitId);

        if (outfitOpt.isEmpty()) {
            event.reply("This outfit could not be found or has been removed.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OutfitEntity outfit = outfitOpt.get();
        outfit.setDisplayCount(outfit.getDisplayCount() + 1);
        outfit.setUpdated(Instant.now());
        outfitRepository.save(outfit);

        int updatedCount = outfit.getDisplayCount();
        Button updatedUpvoteButton = Button.primary(UPVOTE_PREFIX + outfitId, "Upvote (" + updatedCount + ")");
        Button deleteButton = Button.danger(DELETE_PREFIX + outfitId, "Delete");

        event.editComponents(ActionRow.of(updatedUpvoteButton, deleteButton)).queue(
                success -> event.getHook().sendMessage("Thanks for voting! (Outfit `" + outfitId + "` now has " + updatedCount + " votes)")
                        .setEphemeral(true)
                        .queue()
        );
    }

    private void handleDelete(ButtonInteractionEvent event, String outfitId) {
        Optional<OutfitEntity> outfitOpt = outfitRepository.findByIdAndDeletedFalse(outfitId);

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

        outfit.setDeleted(true);
        outfit.setUpdated(Instant.now());
        outfitRepository.save(outfit);
        auditService.record(event.getJDA(), AuditService.Action.OUTFIT_DELETED, userId,
                event.getGuild() != null ? event.getGuild().getId() : null, AuditService.TARGET_OUTFIT, outfitId,
                userId.equals(outfit.getSubmitter()) ? "Deleted by its submitter" : "Deleted by staff (submitter <@" + outfit.getSubmitter() + ">)");

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
