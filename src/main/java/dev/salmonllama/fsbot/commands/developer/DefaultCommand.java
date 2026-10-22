package dev.salmonllama.fsbot.commands.developer;

import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.util.Collections;
import java.util.List;

public class DefaultCommand extends Command {
    @Override public String name() { return "Default"; }
    @Override public String description() { return "The command that gets invoked when the prefix is used, but the command is not recognized"; }
    @Override public String usage() { return "you don't use this command"; }
    @Override public CommandCategory category() { return CommandCategory.DEVELOPER; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.OWNER); }
    @Override public List<String> aliases() { return Collections.singletonList("default"); }

    @Override
    public void onCommand(CommandContext ctx) {
        EmbedBuilder response =  new EmbedBuilder()
                .setTitle("Oops!")
                .setAuthor(ctx.getApi().getYourself())
                .setDescription("That's my prefix, but I don't know that command! Try using `~help` to see what I can do!");

        ctx.reply(response);
    }
}
