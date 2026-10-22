package dev.salmonllama.fsbot.commands.staff;

import dev.salmonllama.fsbot.database.controllers.ServerConfigController;
import dev.salmonllama.fsbot.database.models.ServerConfig;
import dev.salmonllama.fsbot.guthix.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Arrays;
import java.util.List;

public class WelcomeMessageCommand extends Command {
    @Override public String name() { return "Welcome Message"; }
    @Override public String description() { return "View or update the server welcome message. Options: get|set|getchannel|setchannel."; }
    @Override public String usage() { return "welcomemessage <String opt> [String newMessage]"; }
    @Override public CommandCategory category() { return CommandCategory.STAFF; }
    @Override public CommandPermission permission() { return new CommandPermission(PermissionType.STATIC, "staff"); }
    @Override public List<String> aliases() { return Arrays.asList("welcomemessage", "wmsg"); }

    @Override
    public void onCommand(CommandContext ctx) {
        if (ctx.isPrivateMessage()) {
            ctx.reply("This command can only be used within a server");
            return;
        }

        if (ctx.getArgs().length < 1) {
            ctx.reply("You need to supply arguments for this command");
            return;
        }

        String[] args = ctx.getArgs();
        switch (args[0]) {
            case "get":
                get(ctx);
                break;
            case "set": // TODO: check for args here
                String newMsg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                set(ctx, newMsg);
                break;
            default:
                ctx.reply("You used this command incorrectly.");
        }
    }

    private void get(CommandContext ctx) {
        ctx.getServer().ifPresent( // If server is present (not private message) get the server config
                server -> ServerConfigController.get(server.getIdAsString()).thenAcceptAsync(
                        possibleConf -> possibleConf.ifPresentOrElse( // Check for current server config
                                conf -> ctx.reply(getMsg(conf)), // Fetch and send the current welcome message
                                () -> ctx.reply(notFound()) // No welcome message exists, sorrynotsorry
                        )
                ));
    }

    private EmbedBuilder getMsg(ServerConfig config) {
        return new EmbedBuilder()
                .setTitle("Current Welcome Message")
                .setDescription(config.getWelcomeMessage());
    }

    private EmbedBuilder notFound() {
        return new EmbedBuilder()
                .setTitle("Does not exist!")
                .setDescription("No welcome message was found! use `~wmsg set` to set one!");
    }

    private void set(CommandContext ctx, String newMsg) {
        ctx.getServer().ifPresent( // If server is present (private msg check) get the server's config
                server -> ServerConfigController.get(server.getIdAsString()).thenAcceptAsync( // Check for a current server config
                        possibleConf -> possibleConf.ifPresentOrElse(
                                conf -> ctx.reply(updateMsg(conf, newMsg)), // Config exists, update with new welcome message
                                () -> ctx.reply(setNewMsg(server.getIdAsString(), newMsg))))); // Config does not exist, init and add new welcome message
    }

    private EmbedBuilder updateMsg(ServerConfig conf, String newMsg) {
        // Updates a welcome message from an already existing server config
        ServerConfig config = new ServerConfig.ServerConfigBuilder().from(conf)
                .setWelcomeMessage(newMsg)
                .build();

        ServerConfigController.update(config).exceptionally(ExceptionLogger.get()); // TODO: Write a discord Throwable consumer like this

        return new EmbedBuilder()
                .setTitle("Welcome Message Set")
                .addField("New Welcome Message:", config.getWelcomeMessage());
    }

    private EmbedBuilder setNewMsg(String serverId, String newMsg) {
        // Creates a new server config and adds a welcome message
        ServerConfig config = new ServerConfig.ServerConfigBuilder()
                .setId(serverId)
                .setPrefix("~")
                .setWelcomeMessage(newMsg)
                .build();

        ServerConfigController.insert(config);

        return new EmbedBuilder()
                .setTitle("Welcome Message Set!")
                .setDescription("server conf has been created")
                .addField("New Welcome Message:", newMsg);
    }
}
