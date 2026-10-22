/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;


import java.util.Arrays;
import java.util.List;

public class ForceDeleteCommand extends Command {
    @Override public String name() { return "Force Delete"; }
    @Override public String description() { return "Forcefully deletes the given outfit from the database. Destructive action"; }
    @Override public String usage() { return "forcedelete <outfit id>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "owner"); }
    @Override public List<String> aliases() { return Arrays.asList("forcedelete", "sudorm"); }

    @Override
    public void onCommand(CommandContext ctx) {
        // Send a confirmation, confirming the outfit to delete.
        // Confirmation will be done via DM for special confirmation of a destructive action.

        if (ctx.getArgs().length == 0) {
            ctx.reply("You must supply a valid ID");
        }

        var outfitId = ctx.getArgs()[0];

        MessageBuilder builder = new MessageBuilder();
        builder.setContent(
                ":warning: THIS IS A DESTRUCTIVE ACTION :warning:\n\n" +
                        "You are about to destroy an entry from the database.\n\n" +
                        "This is unrecoverable, if the entry in question does not\n" +
                        "break Discord, Fashionscape, or FSBot TOS, please consider using\n" +
                        "`~remove` instead.\n\n" +
                        "Verify the entry's information below and respond with `YES` to proceed\n" +
                        "or `NO` to cancel.\n\n" +
                        "*After 5 minutes, the response window will close and you will have to re-run the command*"
        );

        OutfitController.findById(outfitId).thenAcceptAsync(possibleOutfit -> possibleOutfit.ifPresentOrElse(outfit -> {
            // DM the user with confirmation message, disclaimer, and info embed
            // Attach a 5 minute listener looking for YES or NO
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Outift Information |" + outfit.getId())
                    .setUrl(outfit.getLink())
                    .addField("Submitter:", outfit.getSubmitter())
                    .addField("Meta:", outfit.getMeta())
                    .addField("Link:", outfit.getLink())
                    .addField("Submitted:", outfit.getCreated().toString())
                    .setThumbnail(outfit.getLink())
                    .setFooter(String.format("%s | %s", outfit.getTag(), outfit.getId()));

            builder.setEmbed(embed);

        }, () -> ctx.reply("An outfit with that ID was not found.")));
    }
}
