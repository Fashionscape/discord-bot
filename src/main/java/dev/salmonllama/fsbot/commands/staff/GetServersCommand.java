package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class GetServersCommand extends Command {
    @Override public String name() { return "Get Servers"; }
    @Override public String description() { return "Lists all the servers the bot is in"; }
    @Override public String usage() { return "getservers"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("getservers", "servers"); }

    @Override
    public void onCommand(CommandContext ctx) {
        DiscordApi api = ctx.getApi();

        StringBuilder builder = new StringBuilder();
        builder.append("```md");

        for (Server server : api.getServers()) {
            String serverId = server.getIdAsString();
            String serverName = server.getName();

            builder.append(String.format("\n- %s: %s", serverName, serverId));
        }

        builder.append("\n```");

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("Servers")
                .setDescription(builder.toString())
                .setFooter(String.format("%s is in %d servers", api.getYourself().getName(), api.getServers().size()));

        ctx.getChannel().sendMessage(embed).join();
    }
}
