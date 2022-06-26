/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot.commands.rs3search;

import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionConnection;
import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionSlotRs3;
import dev.salmonllama.fsbot.guthix.*;
import dev.salmonllama.fsbot.utilities.DiscordUtilities;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Rs3NeckCommand extends Command {
    @Override public String name() { return "RS3 Neck"; }
    @Override public String description() { return "Searches scape.fashion for rs3 neck slot items."; }
    @Override public String usage() { return "rs3neck <#color or item name>"; }
    @Override public CommandCategory category() { return CommandCategory.RS3_ITEM_SEARCH; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Collections.singletonList("rs3neck"); }

    private static final Logger logger = LoggerFactory.getLogger(Rs3NeckCommand.class);

    @Override
    public void onCommand(CommandContext ctx) {
        if (ctx.getArgs().length == 0) {
            ctx.reply("Specify something");
            return;
        }

        var args = ctx.getArgs();
        ScapeFashionConnection conn = new ScapeFashionConnection();
        var params = Strings.join(Arrays.asList(args), ' ');

        if (Rs3SearchUtilities.isColor(args[0])) {
            // Color search
            try {
                var bestMatch = conn.rs3Color(params, ScapeFashionSlotRs3.NECK);
                Rs3SearchUtilities.sendResult(bestMatch, ctx.getChannel());
            } catch (Exception e) {
                logger.error(e.getMessage());
                DiscordUtilities.handleException(e, ctx);
            }
        } else {
            // Item search
            try {
                var bestMatch = conn.rs3Item(params, ScapeFashionSlotRs3.NECK);
                Rs3SearchUtilities.sendResult(bestMatch, ctx.getChannel());
            } catch (Exception e) {
                logger.error(e.getMessage());
                DiscordUtilities.handleException(e, ctx);
            }
        }
    }
}
