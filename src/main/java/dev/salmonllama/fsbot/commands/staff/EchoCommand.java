package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.guthix.*;

import java.util.Collections;
import java.util.List;

public class EchoCommand extends Command {
    @Override public String name() { return "Echo"; }
    @Override public String description() { return "Echos your message. Typical bash"; }
    @Override public String usage() { return "echo <message>"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Collections.singletonList("echo"); }

    @Override
    public void onCommand(CommandContext ctx) {
        String echoMessage = String.join(" ", ctx.getArgs());

        ctx.getMessage().delete();

        ctx.reply(echoMessage);
    }
}
