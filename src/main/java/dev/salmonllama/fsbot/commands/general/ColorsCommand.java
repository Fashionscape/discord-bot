package dev.salmonllama.fsbot.commands.general;

import dev.salmonllama.fsbot.guthix.*;

import java.util.Arrays;
import java.util.List;

public class ColorsCommand extends Command {
    @Override public String name() { return "Colors"; }
    @Override public String description() { return "Lists available cosmetic roles"; }
    @Override public String usage() { return "colors"; }
    @Override public CommandCategory category() { return CommandCategory.GENERAL; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Arrays.asList("colors", "colours"); }

    @Override
    public void onCommand(CommandContext ctx) {
        ctx.reply("This command is no longer active. An alternative is currently being developed. For more information, please contact Salmonllama#7233");
    }
}
