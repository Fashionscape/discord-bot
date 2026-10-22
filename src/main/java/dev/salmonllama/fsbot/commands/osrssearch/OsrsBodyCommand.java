package dev.salmonllama.fsbot.commands.osrssearch;

import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionConnection;
import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionSlotOsrs;
import dev.salmonllama.fsbot.guthix.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class OsrsBodyCommand extends Command {
    @Override public String name() { return "OSRS Body"; }
    @Override public String description() { return "Searches scape.fashion for body slot items."; }
    @Override public String usage() { return "osrsbody <#color or item name>"; }
    @Override public CommandCategory category() { return CommandCategory.OSRS_ITEM_SEARCH; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Arrays.asList("07body", "osrsbody"); }

    private static final Logger logger = LoggerFactory.getLogger(OsrsBodyCommand.class);

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
                var bestMatch = conn.osrsColor(params, ScapeFashionSlotOsrs.BODY);
                OsrsSearchUtilities.sendResult(bestMatch, ctx.getChannel());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        } else {
            // Item search
            try {
                var bestMatch = conn.osrsItem(params, ScapeFashionSlotOsrs.BODY);
                OsrsSearchUtilities.sendResult(bestMatch, ctx.getChannel());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }
}
