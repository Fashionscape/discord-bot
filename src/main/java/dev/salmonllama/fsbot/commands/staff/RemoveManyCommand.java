package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.channel.TextChannel;

import java.util.Arrays;
import java.util.List;

public class RemoveManyCommand extends Command {
    @Override public String name() { return "Remove Many Command"; }
    @Override public String description() { return "Removes multiple outfits in one go"; }
    @Override public String usage() { return "removemany <String id>..."; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("removemany", "rmm"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String[] args = ctx.getArgs();
        TextChannel channel = ctx.getChannel();
        long authorId = ctx.getAuthor().getId();

        // Process:
        // Retrieve the outfits.
        // Return error(s) for outfits that don't exist
        // Proceed with outfits that do exist
        //
    }
}
