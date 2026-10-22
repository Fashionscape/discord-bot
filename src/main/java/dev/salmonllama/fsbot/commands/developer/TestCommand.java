package dev.salmonllama.fsbot.commands.developer;

import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionConnection;
import dev.salmonllama.fsbot.guthix.*;

import java.util.Arrays;
import java.util.List;

public class TestCommand extends Command {
    @Override public String name() { return "Test"; }
    @Override public String description() { return "A test command"; }
    @Override public String usage() { return "test"; }
    @Override public CommandCategory category() { return CommandCategory.DEVELOPER; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.OWNER); }
    @Override public List<String> aliases() { return Arrays.asList("test", "t"); }

    @Override
    public void onCommand(CommandContext ctx) {
        ScapeFashionConnection conn = new ScapeFashionConnection();

        try {
            var result = conn.osrsColor("#00ff00");
            ctx.reply(String.format("Best match: %s", result.getItems().get(0).toString()));
        } catch (Exception e) {
            ctx.reply(e.getMessage());
        }
    }
}
