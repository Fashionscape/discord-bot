package dev.salmonllama.fsbot.commands.general;

import dev.salmonllama.fsbot.guthix.*;

import java.util.*;

public class PingCommand extends Command {
    @Override public String name() { return "Ping"; }
    @Override public String description() { return "Pings the bot, checks for a heartbeat"; }
    @Override public String usage() { return "ping"; }
    @Override public CommandCategory category() { return CommandCategory.GENERAL; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.NONE); }
    @Override public List<String> aliases() { return Collections.singletonList("ping"); }

    @Override
    public void onCommand(CommandContext ctx) {
        final long start = System.currentTimeMillis();

        ctx.getChannel().sendMessage("I'm alive").thenAccept(msg -> {
            final long end = System.currentTimeMillis();
            msg.edit(String.format("I'm alive! (%sms)", (end - start)));
        });
    }
}
