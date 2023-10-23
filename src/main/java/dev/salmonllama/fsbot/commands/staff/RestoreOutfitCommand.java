/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.staff;

import com.vdurmont.emoji.EmojiParser;
import dev.salmonllama.fsbot.config.BotConfig;
import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.*;
import java.util.List;

public class RestoreOutfitCommand extends Command {
    @Override public String name() { return "Restore Outfit"; }
    @Override public String description() { return "Restores a previously deleted outfit"; }
    @Override public String usage() { return "restore [outfit ID]"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("restoreoutfit", "restore"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();
        TextChannel channel = ctx.getChannel();
        long authorId = ctx.getUser().getId();

        if (args.length != 1) {
            channel.sendMessage("You must supply a valid outfit ID.");
            return;
        }

        // get the outfit, confirm restoration through reaction.
        String outfitId = args[0];
        OutfitController.findById(outfitId).thenAcceptAsync(possibleOutfit -> possibleOutfit.ifPresentOrElse(outfit -> {
            // Send outfit info, react with selectors, add a listener to the message
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Really restore this outfit?")
                    .setThumbnail(outfit.getLink())
                    .setAuthor(ctx.getApi().getUserById(outfit.getSubmitter()).join())
                    .setUrl(outfit.getLink())
                    .setFooter(String.format("Tag: %s", outfit.getTag()))
                    .addField("Added", outfit.getCreated().toString(), true)
                    .addField("Updated", outfit.getUpdated().toString(), true)
                    .addField("Submitted by:", ctx.getApi().getUserById(outfit.getSubmitter()).join().getDiscriminatedName())
                    .addField("Deleted", outfit.isDeleted() ? "True" : "False", true)
                    .addField("Featured", outfit.isFeatured() ? "True" : "False", true)
                    .addField("Deleted by:", "Lily <3", true);

            ctx.reply(embed).thenAcceptAsync(msg -> {
                msg.addReaction(EmojiParser.parseToUnicode(":white_check_mark:"));
                msg.addReaction(EmojiParser.parseToUnicode(":octagonal_sign:"));

                msg.addReactionAddListener(event -> {
                    if (event.getUserId() != authorId) {
                        return;
                    }

                    if (event.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) {
                        // Restore the outfit
                        OutfitController.restore(outfit);

                        EmbedBuilder response = new EmbedBuilder()
                                .setTitle("Outfit Restored!")
                                .setDescription(String.format("Outfit %s is now active again!", outfit.getId()));

                        msg.delete();
                        ctx.reply(response);

                        EmbedBuilder log = new EmbedBuilder()
                                .setTitle("Outfit Restored as Active")
                                .setFooter(outfit.getId())
                                .setThumbnail(outfit.getLink())
                                .setColor(Color.BLUE)
                                .addField("Restored By:", ctx.getAuthor().getDiscriminatedName());

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

                    } else if (event.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":octagonal_sign:"))) {
                        // Do nothing
                        EmbedBuilder response = new EmbedBuilder()
                                .setTitle("Restoration Aborted")
                                .setDescription(String.format("No modifications were made to %s", outfit.getId()));

                        ctx.reply(response);
                    }
                });
            });
        }, () -> {
            EmbedBuilder response = new EmbedBuilder()
                    .setTitle("Outfit not Found")
                    .setDescription(String.format("ID %s does not exist", outfitId));

            ctx.reply(response);
        }));
    }
}
