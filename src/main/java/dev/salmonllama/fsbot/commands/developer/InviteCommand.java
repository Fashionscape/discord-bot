package dev.salmonllama.fsbot.commands.developer;

import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;

import java.util.Arrays;
import java.util.List;

public class InviteCommand extends Command {
    @Override public String name() { return "Invite"; }
    @Override public String description() { return "Spits out a bot invite"; }
    @Override public String usage() { return "invite"; }
    @Override public CommandCategory category() { return CommandCategory.DEVELOPER; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "owner"); }
    @Override public List<String> aliases() { return Arrays.asList("invite", "inv"); }

    @Override
    public void onCommand(CommandContext ctx) {
        TextChannel channel = ctx.getChannel();
        DiscordApi api = ctx.getApi();

        channel.sendMessage(String.format("<%s>", api.createBotInvite()));
    }
}
