package dev.salmonllama.fsbot.guthix;

import dev.salmonllama.fsbot.commands.developer.*;
import dev.salmonllama.fsbot.commands.general.*;
import dev.salmonllama.fsbot.commands.osrssearch.*;
import dev.salmonllama.fsbot.commands.rs3search.*;
import dev.salmonllama.fsbot.commands.staff.OutfitInfoCommand;
import dev.salmonllama.fsbot.commands.staff.*;
import dev.salmonllama.fsbot.database.controllers.UserBlacklistController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;

/*
 * Guthix is Fashionscape Bot's command repository and dispatcher
 */
public class Guthix implements MessageCreateListener {
    private final static Logger logger = LoggerFactory.getLogger(Guthix.class);

    @SuppressWarnings("unused")
    private final DiscordApi api;

    private final Registry registry;
    private final PermissionManager manager;

    public Guthix(DiscordApi api) {
        this.api = api;
        api.addMessageCreateListener(this);

        manager = new PermissionManager();
        registry = new Registry();

        initCommands();
    }

    public void initCommands() {
        logger.info("Initializing commands...");

        // Developer Commands
        addCommand(new TestCommand());
        addCommand(new CreateGalleryCommand());
        addCommand(new InviteCommand());
        addCommand(new PermissionCommand());

        // Staff Commands
        addCommand(new EchoCommand());
        addCommand(new GetServersCommand());
        addCommand(new AddColorCommand());
        addCommand(new GetOutfitCommand());
        addCommand(new RetagCommand());
        addCommand(new RemoveOutfitCommand());
        addCommand(new OutfitInfoCommand());
        addCommand(new SetStatusCommand());
        addCommand(new WelcomeMessageCommand());
        addCommand(new ShowGalleriesCommand());
        addCommand(new EditMetaCommand());
        addCommand(new BlacklistUserCommand());
        addCommand(new RestoreOutfitCommand());

        // General Commands
        addCommand(new PingCommand());
        addCommand(new ColorCommand());
        addCommand(new ColorsCommand());
        addCommand(new OutfitCommand());
        addCommand(new HelpCommand(this));
        addCommand(new StatsCommand());
        addCommand(new PrivacyCommand());

        // Osrs Search Commands
        addCommand(new OsrsSearchCommand());
        addCommand(new OsrsHeadCommand());
        addCommand(new OsrsBodyCommand());
        addCommand(new OsrsHandCommand());
        addCommand(new OsrsLegsCommand());
        addCommand(new OsrsFeetCommand());
        addCommand(new OsrsCapeCommand());
        addCommand(new OsrsNeckCommand());
        addCommand(new OsrsShieldCommand());
        addCommand(new OsrsWeaponCommand());

        // Rs3 Search Commands
        addCommand(new Rs3SearchCommand());
        addCommand(new Rs3HeadCommand());
        addCommand(new Rs3BodyCommand());
        addCommand(new Rs3NeckCommand());
        addCommand(new Rs3HandCommand());
        addCommand(new Rs3FeetCommand());
        addCommand(new Rs3LegsCommand());
        addCommand(new Rs3BackCommand());
        addCommand(new Rs3MainhandCommand());
        addCommand(new Rs3OffhandCommand());
        addCommand(new Rs3TwohandedCommand());

        logger.info("Command initialization complete!");
    }

    public void addCommand(Command cmd) {
        registry.addCommand(cmd);
    }

    public Collection<Command> listCommands() {
        return registry.listCommands();
    }

    public HashMap<String, Command> mapCommands() {
        return registry.mapCommands();
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        MessageAuthor author = event.getMessageAuthor();

        if (manager.sourceIsValid(author)) {

        } else {
            return;
        }

        String content = event.getMessageContent();
        String contentLower = content.toLowerCase();

        if (registry.startsWithPrefix(contentLower)) {

        } else {
            return;
        }

        // Check for blacklisted user
        if (!UserBlacklistController.exists(author.getIdAsString()).join()) {
            // User is not blacklisted, continue as normal
        } else {
            return;
        }

        RegistryCommand rComm = registry.getCommandInfo(content);
        String cmdString = rComm.getCommand().toLowerCase();

        String[] cmdArgs = rComm.getArgs();

        Command cmd = registry.findCommand(cmdString).orElse(new DefaultCommand());

        CommandContext ctx = new CommandContext.CommandContextBuilder(
                event,
                cmd,
                cmdString,
                cmdArgs
        ).build();

        if (manager.userHasPermission(cmd.permission(), ctx)) {

        } else {
            return;
        }

        cmd.invoke(ctx);
        logger.info(String.format("Processed command from %s.", author.getDiscriminatedName()));
    }
}
