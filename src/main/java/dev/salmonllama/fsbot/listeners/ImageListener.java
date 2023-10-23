/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.listeners;

import com.vdurmont.emoji.EmojiParser;
import dev.salmonllama.fsbot.config.BotConfig;
import dev.salmonllama.fsbot.database.controllers.GalleryController;
import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.database.controllers.UserBlacklistController;
import dev.salmonllama.fsbot.database.models.Outfit;
import dev.salmonllama.fsbot.endpoints.imgur.ImgurAPIConnection;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

import java.util.UUID;

public class ImageListener implements MessageCreateListener {
    private static final Logger logger = LoggerFactory.getLogger(ImageListener.class);

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        // Check for valid source -> DONE -> WORKING
        // Check for gallery channel presence -> DONE -> WORKING
        // Check for images (attached files and links from approved sources) -> DONE -> WORKING (approved links to be added later)
        // Upload the image(s) to imgur -> DONE -> WORKING
        // Store the image in the database -> DONE -> WORKING
        // Send confirmation && log event -> IN PROGRESS (waiting for logger upgrade)
        // Check for production environment to avoid uploading dev images to Imgur -> DONE -> WORKING

        if (!event.getMessageAuthor().isRegularUser()) {
            // Ignore anything that is a webhook or a bot message
            return;
        }

        if (UserBlacklistController.exists(event.getMessageAuthor().getIdAsString()).join()) {
            return;
        }

        // Only works in Server Text Channels
        event.getChannel().asServerTextChannel().ifPresent(channel -> {
            // Only works in registered Gallery Channels
            GalleryController.galleryExists(channel.getIdAsString()).thenAccept(exists -> {
                if (exists) {
                    // Check the message for images
                    if (event.getMessageAttachments().stream().anyMatch(MessageAttachment::isImage)) {
                        event.getMessageAttachments().stream().filter(MessageAttachment::isImage).forEach(image -> {
                            // Check the ENVIRONMENT env-var. If PROD -> Upload to imgur and store. If not, just store
                            // Upload the image(s) to Imgur, store in database, log the stored images.

                            if (System.getenv("ENVIRONMENT") != null) {
                                // Upload the image(s) to Imgur, store in database, log the stored images.
                                uploadAndStore(event, channel, image);
                            } else {
                                // Store the image(s) in database, log the stored images.
                                store(event, channel, image);
                            }
                        });
                    }
                }
            }).exceptionally(ExceptionLogger.get());
        });
    }

    private void uploadAndStore(MessageCreateEvent event, ServerTextChannel channel, MessageAttachment image) {
        ImgurAPIConnection imgur = new ImgurAPIConnection();

        // Upload to Imgur
        imgur.uploadImage(image.getUrl().toString()).thenAccept(upload -> {
            // Store in the database
            Outfit.OutfitBuilder outfitBuilder = new Outfit.OutfitBuilder()
                    .setId(upload.getId())
                    .setMeta(event.getMessageContent())
                    .setDiscordName(event.getMessageAuthor().getDiscriminatedName())
                    .setLink(upload.getLink())
                    .setSubmitter(event.getMessageAuthor().getIdAsString())
                    .setDeleteHash(upload.getDeleteHash());

            storeAndLog(event, channel, outfitBuilder);
        }).exceptionally(e -> {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("Error!")
                    .setColor(Color.RED)
                    .setAuthor(event.getApi().getYourself())
                    .setDescription(e.getMessage());

            event.getChannel().sendMessage(errorEmbed);
            return null;
        });
    }

    private void store(MessageCreateEvent event, ServerTextChannel channel, MessageAttachment image) {
        // Store in the database
        Outfit.OutfitBuilder outfitBuilder = new Outfit.OutfitBuilder()
                .setId(UUID.randomUUID().toString())
                .setMeta(event.getMessageContent())
                .setDiscordName(event.getMessageAuthor().getDiscriminatedName())
                .setLink(event.getMessageAttachments().get(0).getUrl().toString())
                .setSubmitter(event.getMessageAuthor().getIdAsString())
                .setDeleteHash("DUMMY-DELETE-HASH");

        storeAndLog(event, channel, outfitBuilder);
    }

    private void storeAndLog(MessageCreateEvent event, ServerTextChannel channel, Outfit.OutfitBuilder outfitBuilder) {
        GalleryController.getTag(channel.getIdAsString()).thenAccept(tag -> {
            outfitBuilder.setTag(tag);
            Outfit outfit = outfitBuilder.build();

            OutfitController.insert(outfit).thenAcceptAsync((Void) -> {
                // Log the outfit

                // TODO: START ZAMMY LOG
                if (outfit.getTag().equals("necro-contest"))
                {
                    event.getApi().getServerTextChannelById(BotConfig.CONTEST_LOG).ifPresentOrElse(chnl ->
                    {
                        EmbedBuilder response = new EmbedBuilder()
                                .setTitle("Outfit Added")
                                .setAuthor(event.getMessageAuthor())
                                .setThumbnail(outfit.getLink())
                                .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                                .setUrl(outfit.getLink())
                                .setColor(Color.GREEN)
                                .addField("Uploaded:", outfit.getCreated().toString())
                                .addField("Discord Name:", outfit.getDiscordName());

                        if (!outfit.getMeta().equals("")) {
                            response.addField("Meta:", outfit.getMeta());
                        }

                        chnl.sendMessage(response);
                        logger.info(String.format("Outfit from %s successfully added to the running event.", event.getMessageAuthor().getDiscriminatedName()));

                        // Add the reaction to the original message
                        GalleryController.getEmoji(channel.getIdAsString()).thenAcceptAsync(
                                emoji -> event.getMessage().addReaction(EmojiParser.parseToUnicode(emoji))
                        ).exceptionally(ExceptionLogger.get());
                    }, () ->
                    {
                        // Fallback error message to me
                        event.getApi().getUserById(BotConfig.BOT_OWNER).thenAcceptAsync(
                                user -> user.sendMessage("Could not find CONTEST_LOG")
                        );
                    });
                }
                // TODO: END ZAMMY LOG

                event.getApi().getServerTextChannelById(BotConfig.OUTFIT_LOG).ifPresentOrElse(chnl -> {
                    EmbedBuilder response = new EmbedBuilder()
                            .setTitle("Outfit Added")
                            .setAuthor(event.getMessageAuthor())
                            .setThumbnail(outfit.getLink())
                            .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                            .setUrl(outfit.getLink())
                            .setColor(Color.GREEN)
                            .addField("Uploaded:", outfit.getCreated().toString())
                            .addField("Discord Name:", outfit.getDiscordName());

                    if (!outfit.getMeta().equals("")) {
                        response.addField("Meta:", outfit.getMeta());
                    }

                    chnl.sendMessage(response);
                    logger.info(String.format("Outfit from %s successfully added.", event.getMessageAuthor().getDiscriminatedName()));

                    // Add the reaction to the original message
                    GalleryController.getEmoji(channel.getIdAsString()).thenAcceptAsync(
                            emoji -> event.getMessage().addReaction(EmojiParser.parseToUnicode(emoji))
                    ).exceptionally(ExceptionLogger.get());
                }, () -> {
                    // Fallback error message to me
                    event.getApi().getUserById(BotConfig.BOT_OWNER).thenAcceptAsync(
                            user -> user.sendMessage("Could not find OUTFIT LOG")
                    );
                });
            });
        });
    }
}

