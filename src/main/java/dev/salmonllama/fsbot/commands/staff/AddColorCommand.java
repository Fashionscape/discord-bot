package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.guthix.*;

import java.util.Arrays;
import java.util.List;

public class AddColorCommand extends Command {
    @Override public String name() { return "Add Color"; }
    @Override public String description() { return "adds the provided role to the toggleable cosmetic roles."; }
    @Override public String usage() { return "addcolor <colorName> <roleId>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("addcolor", "addcolour", "addclr"); }

    @Override
    public void onCommand(CommandContext ctx) {
//        ctx.getApi().getOwner().thenAcceptAsync(owner -> {
//            ctx.reply("This command is no longer active. An alternative is currently being developed. For more information, please contact " + owner.getDiscriminatedName());
//        });
    }
}
