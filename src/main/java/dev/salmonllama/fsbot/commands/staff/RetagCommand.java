/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.staff;

import com.vdurmont.emoji.EmojiParser;
import dev.salmonllama.fsbot.config.BotConfig;
import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.database.models.Outfit;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public class RetagCommand extends Command {
    @Override public String name() { return "Retag"; }
    @Override public String description() { return "Changes the tag of the given outfit"; }
    @Override public String usage() { return "retag <String id> <String newtag>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Collections.singletonList("retag"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();
        TextChannel channel = ctx.getChannel();
        long authorId = ctx.getUser().getId();

        if (args.length != 2) {
            channel.sendMessage("Improper usage");
            return;
        }

        // Get the outfit, confirm update through reaction
        String outfitId = args[0];
        String newTag = args[1];

        OutfitController.findById(outfitId).thenAcceptAsync(possibleOutfit -> possibleOutfit.ifPresentOrElse(outfit -> {
            // Send info, confirmation, and add reaction listener
            EmbedBuilder response = new EmbedBuilder()
                    .setTitle("Confirm Tag Edit")
                    .setThumbnail(outfit.getLink())
                    .setAuthor(ctx.getApi().getUserById(outfit.getSubmitter()).join())
                    .setUrl(outfit.getLink())
                    .addField("Current Tag:", outfit.getTag())
                    .addField("New Tag:", newTag);

            ctx.reply(response).thenAcceptAsync(msg -> {
                msg.addReaction(EmojiParser.parseToUnicode(":white_check_mark:"));
                msg.addReaction(EmojiParser.parseToUnicode(":octagonal_sign:"));

                msg.addReactionAddListener(event -> {
                    if (event.getUserId() != authorId) {
                        return;
                    }

                    if (event.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) {
                        // Update the outfit
                        Outfit newOutfit = new Outfit.OutfitBuilder(outfit)
                                .setTag(newTag)
                                .setUpdated(new Timestamp(System.currentTimeMillis()))
                                .build();

                        OutfitController.update(newOutfit).thenAcceptAsync((Void) -> {
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle("Outfit retagged successfully!")
                                    .setDescription(String.format("Outfit %s will now display as %s", newOutfit.getId(), newOutfit.getTag()));

                            msg.delete();
                            ctx.reply(embed);

                            EmbedBuilder log = new EmbedBuilder()
                                    .setTitle("Outfit Retagged")
                                    .setFooter(outfit.getId())
                                    .setColor(Color.YELLOW)
                                    .setThumbnail(outfit.getLink())
                                    .addField("New tag:", newTag);

                            // TODO: START ZAMMY
                            if (outfit.getTag().equals("necro-contest"))
                            {
                                ctx.getApi().getServerTextChannelById(BotConfig.CONTEST_LOG).ifPresent(
                                        chnl -> chnl.sendMessage(log)
                                );
                            }
                            // TODO: END ZAMMY

                            ctx.getApi().getServerTextChannelById(BotConfig.OUTFIT_LOG).ifPresent(
                                    chnl -> chnl.sendMessage(log)
                            );
                        });

                    } else if (event.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":octagonal_sign:"))) {
                        // Do nothing
                        msg.delete();
                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle("Update Cancelled")
                                .setDescription("No modifications were made");

                        ctx.reply(embed);
                    }
                });
            });
        }, () -> {
            // Err, outfit not found
            EmbedBuilder response = new EmbedBuilder()
                    .setTitle("Error occurred")
                    .setDescription("That ID was not found in the database")
                    .setColor(Color.RED);

            ctx.reply(response);
        }));
    }
}
