package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.config.BotConfig;
import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.database.models.Outfit;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.List;
import java.util.Arrays;

public class BulkCommand extends Command {
    @Override public String name() { return "Bulk remove/retag commands"; }
    @Override public String description() { return "Bulk removes or retags outfits"; }
    @Override public String usage() { return "bulk remove/retag (if retag) <String newtag> <List<String> ids>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("bulk"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();
        TextChannel channel = ctx.getChannel();
        long authorId = ctx.getUser().getId();

        if (args.length < 2) {
            channel.sendMessage("Please provide multiple IDs.");
            return;
        }

        switch(args[0]) {
            case "remove":
                for (int i = 1; i < args.length; i++) {
                    String outfitId = args[i];
                    OutfitController.findById(outfitId).thenAcceptAsync(possibleOutfit -> possibleOutfit.ifPresentOrElse(outfit -> {
                        OutfitController.delete(outfit.getId());

                        EmbedBuilder response = new EmbedBuilder()
                                .setTitle("Deletion Successful!")
                                .setDescription(String.format("Outfit %s marked as deleted", outfit.getId()));
                        ctx.reply(response);

                        EmbedBuilder log = new EmbedBuilder()
                                .setTitle("Outfit Marked as Deleted")
                                .setFooter(outfit.getId())
                                .setThumbnail(outfit.getLink())
                                .setColor(Color.RED)
                                .addField("Deleted By:", ctx.getAuthor().getDiscriminatedName());

                        ctx.getApi().getServerTextChannelById(BotConfig.OUTFIT_LOG).ifPresent(
                                chnl -> chnl.sendMessage(log)
                        );
                    }, () -> {
                        EmbedBuilder response = new EmbedBuilder()
                                .setTitle("Outfit not found")
                                .setDescription(String.format("ID %s does not exist", outfitId));
                        ctx.reply(response);
                    }));
                }
                break;
            case "retag":
                String newTag = args[1];
                for (int i = 2; i < args.length; i++) {
                    String outfitId = args[i];
                    OutfitController.findById(outfitId).thenAcceptAsync(possibleOutfit -> possibleOutfit.ifPresentOrElse(outfit -> {
                        Outfit newOutfit = new Outfit.OutfitBuilder(outfit)
                                .setTag(newTag)
                                .setUpdated(new Timestamp(System.currentTimeMillis()))
                                .build();
                        OutfitController.update(newOutfit).thenAcceptAsync((Void) -> {
                            EmbedBuilder response = new EmbedBuilder()
                                    .setTitle("Outfit retagged successfully!")
                                    .setDescription(String.format("Outfit %s will now display as %s", newOutfit.getId(), newOutfit.getTag()));
                            ctx.reply(response);

                            EmbedBuilder log = new EmbedBuilder()
                                    .setTitle("Outfit Retagged")
                                    .setFooter(outfit.getId())
                                    .setColor(Color.YELLOW)
                                    .setThumbnail(outfit.getLink())
                                    .addField("New tag:", newTag);

                            ctx.getApi().getServerTextChannelById(BotConfig.OUTFIT_LOG).ifPresent(
                                    chnl -> chnl.sendMessage(log)
                            );
                        });

                    }, () -> {
                        EmbedBuilder response = new EmbedBuilder()
                                .setTitle("Outfit not found")
                                .setDescription(String.format("ID %s does not exist", outfitId));
                        ctx.reply(response);
                    }));
                }
                break;
            default:
                channel.sendMessage("Only remove & retag are supported with the bulk command.");
                break;
        }
    }

}