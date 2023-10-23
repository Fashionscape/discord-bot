/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.staff;

import com.vdurmont.emoji.EmojiParser;
import dev.salmonllama.fsbot.config.BotConfig;
import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.guthix.*;
import org.apache.logging.log4j.util.Strings;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class EditMetaCommand extends Command {
    @Override public String name() { return "Edit Meta"; }
    @Override public String description() { return "Edit the meta information on the given outfit"; }
    @Override public String usage() { return "editmeta <String id> <String newMeta>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("editmeta", "emeta"); }

    @Override
    public void onCommand(CommandContext ctx) {
        var args = ctx.getArgs();
        var newMeta = Strings.join(Arrays.asList(Arrays.copyOfRange(args, 1, args.length)), ' ');
        var channel = ctx.getChannel();
        var authorId = ctx.getUser().getId();

        if (args.length != 1) {
            channel.sendMessage("You must supply a valid outfit ID.");
            return;
        }

        // get the outfit, confirm meta-edit through confirmation
        String outfitId = args[0];
        OutfitController.findById(outfitId).thenAcceptAsync(possibleOutfit -> possibleOutfit.ifPresentOrElse(outfit -> {
            // Send outfit info, react with selectors, add a listener to the message
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Confirm Meta Edit")
                    .setThumbnail(outfit.getLink())
                    .setAuthor(ctx.getApi().getUserById(outfit.getSubmitter()).join())
                    .setUrl(outfit.getLink())
                    .setFooter(String.format("Tag: %s", outfit.getTag()))
                    .addField("Submitted by:", ctx.getApi().getUserById(outfit.getSubmitter()).join().getDiscriminatedName())
                    .addField("Current Meta", outfit.getMeta())
                    .addField("Proposed Meta", newMeta);

            ctx.reply(embed).thenAcceptAsync(msg -> {
                msg.addReaction(EmojiParser.parseToUnicode(":white_check_mark:"));
                msg.addReaction(EmojiParser.parseToUnicode(":octagonal_sign:"));

                msg.addReactionAddListener(event -> {
                    if (event.getUserId() != authorId) {
                        return;
                    }

                    if (event.getEmoji().equalsEmoji(EmojiParser.parseToUnicode(":white_check_mark:"))) {
                        // Edit the outfit's meta

                        outfit.setMeta(newMeta);
                        OutfitController.update(outfit);

                        EmbedBuilder response = new EmbedBuilder()
                                .setTitle("Meta Modification Successful!")
                                .setDescription(String.format("New Meta: %s", outfit.getMeta()));

                        msg.delete();
                        ctx.reply(response);

                        EmbedBuilder log = new EmbedBuilder()
                                .setTitle("Outfit Meta Changed")
                                .setThumbnail(outfit.getLink())
                                .setColor(Color.YELLOW)
                                .addField("Edited By:", ctx.getAuthor().getDiscriminatedName());

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
                                .setTitle("Meta Modification Aborted")
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
