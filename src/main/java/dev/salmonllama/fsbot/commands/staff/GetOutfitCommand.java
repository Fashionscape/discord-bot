package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.database.controllers.OutfitController;
import dev.salmonllama.fsbot.guthix.*;

import java.util.Arrays;
import java.util.List;

public class GetOutfitCommand extends Command {
    @Override public String name() { return "Get Outift"; }
    @Override public String description() { return "Shows the outfit, given an ID"; }
    @Override public String usage() { return "getoutfit <String id>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("getoutfit", "get"); }

    @Override
    public void onCommand(CommandContext ctx) {
        // args is ONLY the ID
        String[] args = ctx.getArgs();

        String id = args[0];
        OutfitController.findById(id).thenAccept(outfitOpt -> outfitOpt.ifPresentOrElse(
            outfit -> ctx.reply(outfit.toString()),
            () -> ctx.reply("Outfit not found, did you get the id right?")
        ));
    }
}
