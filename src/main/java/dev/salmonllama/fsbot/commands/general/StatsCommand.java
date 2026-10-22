package dev.salmonllama.fsbot.commands.general;

import java.util.*;

import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import dev.salmonllama.fsbot.database.controllers.OutfitController;

public class StatsCommand extends Command {
    @Override public String name() { return "Stats"; }
    @Override public String description() { return "Shows various stats from Fashionscape Bot"; }
    @Override public String usage() { return "stats"; }
    @Override public CommandCategory category() { return CommandCategory.GENERAL; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Collections.singletonList("stats"); }

    @Override
    public void onCommand(CommandContext ctx) {
        // Stats to display:
        // Number of users
        // Number of servers
        // Number of galleries
        // Number of non-deleted images
        // Number of total images
        // CPU and RAM?
        int userCount = ctx.getApi().getCachedUsers().size(); // Will these be accurate with sharding?
        int serverCount = ctx.getApi().getServers().size();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Stats");
        embed.addField("Users:", String.valueOf(userCount));
        embed.addField("Servers:", String.valueOf(serverCount));

        OutfitController.countActiveOutfits().thenAccept(count -> embed.addField("Active outfits:", String.valueOf(count))).join();
        OutfitController.countTotalOutfits().thenAccept(count -> embed.addField("Total outifts:", String.valueOf(count))).join();
        OutfitController.countFeaturedOutfits().thenAccept(count -> embed.addField("Featured outfits:", String.valueOf(count))).join();

        ctx.reply(embed);
    }
}