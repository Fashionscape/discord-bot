/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.osrssearch;

import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionConnection;
import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionSlotOsrs;
import dev.salmonllama.fsbot.guthix.*;
import dev.salmonllama.fsbot.utilities.DiscordUtilities;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class OsrsNeckCommand extends Command {
    @Override public String name() { return "OSRS Neck"; }
    @Override public String description() { return "Searches scape.fashion for neck slot items."; }
    @Override public String usage() { return "osrsneck <#color or item name>"; }
    @Override public CommandCategory category() { return CommandCategory.OSRS_ITEM_SEARCH; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Arrays.asList("07neck", "osrsneck"); }

    private static final Logger logger = LoggerFactory.getLogger(OsrsNeckCommand.class);

    @Override
    public void onCommand(CommandContext ctx) {
        if (ctx.getArgs().length == 0) {
            ctx.reply("Specify something");
            return;
        }

        var args = ctx.getArgs();
        ScapeFashionConnection conn = new ScapeFashionConnection();
        var params = Strings.join(Arrays.asList(args), ' ');

        if (OsrsSearchUtilities.isColor(args[0])) {
            // Color search
            try {
                var bestMatch = conn.osrsColor(params, ScapeFashionSlotOsrs.NECK);
                OsrsSearchUtilities.sendResult(bestMatch, ctx.getChannel());
            } catch (Exception e) {
                logger.error(e.getMessage());
                DiscordUtilities.handleException(e, ctx);
            }
        } else {
            // Item search
            try {
                var bestMatch = conn.osrsItem(params, ScapeFashionSlotOsrs.NECK);
                OsrsSearchUtilities.sendResult(bestMatch, ctx.getChannel());
            } catch (Exception e) {
                logger.error(e.getMessage());
                DiscordUtilities.handleException(e, ctx);
            }
        }
    }
}
