/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.listeners;

import dev.salmonllama.fsbot.config.BotConfig;
import dev.salmonllama.fsbot.database.controllers.GalleryController;
import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.database.controllers.UserBlacklistController;
import dev.salmonllama.fsbot.database.models.Outfit;
import dev.salmonllama.fsbot.endpoints.imgur.ImgurAPIConnection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

import java.util.UUID;

public class ImageListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ImageListener.class);

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Check for valid source -> DONE -> WORKING
        // Check for gallery channel presence -> DONE -> WORKING
        // Check for images (attached files and links from approved sources) -> DONE -> WORKING (approved links to be added later)
        // Upload the image(s) to imgur -> DONE -> WORKING
        // Store the image in the database -> DONE -> WORKING
        // Send confirmation && log event -> IN PROGRESS (waiting for logger upgrade)
        // Check for production environment to avoid uploading dev images to Imgur -> DONE -> WORKING

        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
            // Ignore anything that is a webhook or a bot message
            return;
        }

        if (UserBlacklistController.exists(event.getAuthor().getId()).join()) {
            return;
        }

        // Only works in Server Text Channels
        var channel = event.getChannel().asTextChannel();

        // Only works in registered Gallery Channels
        GalleryController.galleryExists(channel.getId()).thenAccept(exists -> {
            if (exists) {
                // Check the message for images
                if (event.getMessage().getAttachments().stream().anyMatch(Message.Attachment::isImage)) {
                    event.getMessage().getAttachments().stream().filter(Message.Attachment::isImage).forEach(image -> {
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
        });
    }

    private void uploadAndStore(MessageReceivedEvent event, TextChannel channel, Message.Attachment image) {
        ImgurAPIConnection imgur = new ImgurAPIConnection();

        // Upload to Imgur
        imgur.uploadImage(image.getUrl()).thenAccept(upload -> {
            // Store in the database
            Outfit.OutfitBuilder outfitBuilder = new Outfit.OutfitBuilder()
                    .setId(upload.getId())
                    .setMeta(event.getMessage().getContentRaw())
                    .setDiscordName(event.getAuthor().getName())
                    .setLink(upload.getLink())
                    .setSubmitter(event.getAuthor().getId())
                    .setDeleteHash(upload.getDeleteHash());

            storeAndLog(event, channel, outfitBuilder);
        }).exceptionally(e -> {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("Error!")
                    .setColor(Color.RED)
                    .setAuthor(event.getJDA().getSelfUser().getName(), null, event.getJDA().getSelfUser().getAvatarUrl())
                    .setDescription(e.getMessage());

            event.getChannel().sendMessageEmbeds(errorEmbed.build()).queue();
            return null;
        });
    }

    private void store(MessageReceivedEvent event, TextChannel channel, Message.Attachment image) {
        // Store in the database
        Outfit.OutfitBuilder outfitBuilder = new Outfit.OutfitBuilder()
                .setId(UUID.randomUUID().toString())
                .setMeta(event.getMessage().getContentRaw())
                .setDiscordName(event.getAuthor().getName())
                .setLink(event.getMessage().getAttachments().get(0).getUrl())
                .setSubmitter(event.getAuthor().getId())
                .setDeleteHash("DUMMY-DELETE-HASH");

        storeAndLog(event, channel, outfitBuilder);
    }

    private void storeAndLog(MessageReceivedEvent event, TextChannel eventChannel, Outfit.OutfitBuilder outfitBuilder) {
        GalleryController.getTag(eventChannel.getId()).thenAccept(tag -> {
            outfitBuilder.setTag(tag);
            Outfit outfit = outfitBuilder.build();

            OutfitController.insert(outfit).thenAcceptAsync((Void) -> {
                // Log the outfit

//                // TODO: START ZAMMY LOG
//                if (outfit.getTag().equals("necro-contest"))
//                {
//                    event.getJDA().getTextChannelById(BotConfig.CONTEST_LOG).ifPresentOrElse(chnl ->
//                    {
//                        EmbedBuilder response = new EmbedBuilder()
//                                .setTitle("Outfit Added")
//                                .setAuthor(event.getMessageAuthor())
//                                .setThumbnail(outfit.getLink())
//                                .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
//                                .setUrl(outfit.getLink())
//                                .setColor(Color.GREEN)
//                                .addField("Uploaded:", outfit.getCreated().toString())
//                                .addField("Discord Name:", outfit.getDiscordName());
//
//                        if (!outfit.getMeta().equals("")) {
//                            response.addField("Meta:", outfit.getMeta());
//                        }
//
//                        chnl.sendMessage(response);
//                        logger.info(String.format("Outfit from %s successfully added to the running event.", event.getMessageAuthor().getDiscriminatedName()));
//
//                        // Add the reaction to the original message
//                        GalleryController.getEmoji(channel.getIdAsString()).thenAcceptAsync(
//                                emoji -> event.getMessage().addReaction(EmojiParser.parseToUnicode(emoji))
//                        ).exceptionally(ExceptionLogger.get());
//                    }, () ->
//                    {
//                        // Fallback error message to me
//                        event.getApi().getUserById(BotConfig.BOT_OWNER).thenAcceptAsync(
//                                user -> user.sendMessage("Could not find CONTEST_LOG")
//                        );
//                    });
//                }
//                // TODO: END ZAMMY LOG

                var logChannel = event.getJDA().getTextChannelById(BotConfig.OUTFIT_LOG);
                if (logChannel != null) {
                    EmbedBuilder response = new EmbedBuilder()
                            .setTitle("Outfit Added")
                            .setAuthor(event.getAuthor().getName(), null, event.getAuthor().getAvatarUrl())
                            .setThumbnail(outfit.getLink())
                            .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()))
                            .setUrl(outfit.getLink())
                            .setColor(Color.GREEN)
                            .addField("Uploaded:", outfit.getCreated().toString(), false)
                            .addField("Discord Name:", outfit.getDiscordName(), false);

                    if (!outfit.getMeta().isEmpty()) {
                        response.addField("Meta:", outfit.getMeta(), false);
                    }

                    logChannel.sendMessageEmbeds(response.build()).queue();
                    logger.info(String.format("Outfit from %s successfully added.", event.getAuthor().getName()));

                    // Add the emoji reaction to the message
                    GalleryController.getEmoji(eventChannel.getId()).thenAcceptAsync(
                            emoji -> event.getMessage().addReaction(Emoji.fromUnicode(emoji))
                    );
                } else {
                    // TODO: Fallback error logging in owner DMs
                    // May need to wrap the JDA object in a further abstracted object that contains owner attributes
                }
            });
        });
    }
}

