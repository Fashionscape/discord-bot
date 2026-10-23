package dev.salmonllama.fsbot.discord.commands;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.discord.components.ButtonHandler;
import dev.salmonllama.fsbot.services.BlocklistService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SlashCommandDispatcher extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SlashCommandDispatcher.class);

    private final Map<String, SlashCommand> commandRegistry = new ConcurrentHashMap<>();
    private final List<ButtonHandler> buttonHandlers;
    private final BlocklistService blocklistService;
    private final BotProperties properties;

    public SlashCommandDispatcher(
            List<SlashCommand> commands,
            List<ButtonHandler> buttonHandlers,
            BlocklistService blocklistService,
            BotProperties properties
    ) {
        this.buttonHandlers = buttonHandlers != null ? buttonHandlers : List.of();
        this.blocklistService = blocklistService;
        this.properties = properties;

        if (commands != null) {
            for (SlashCommand cmd : commands) {
                registerCommand(cmd);
            }
        }
    }

    public void registerCommand(SlashCommand command) {
        commandRegistry.put(command.getName().toLowerCase(Locale.ROOT), command);
        logger.info("Registered slash command: /{}", command.getName());
    }

    public Optional<SlashCommand> getCommand(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(commandRegistry.get(name.toLowerCase(Locale.ROOT)));
    }

    public Collection<SlashCommand> getRegisteredCommands() {
        return Collections.unmodifiableCollection(commandRegistry.values());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("JDA gateway ready! Logged in as: {}", event.getJDA().getSelfUser().getAsTag());

        String devGuildId = properties.discord() != null ? properties.discord().devGuildId() : null;
        if (devGuildId != null && !devGuildId.isBlank()) {
            Guild devGuild = event.getJDA().getGuildById(devGuildId);
            if (devGuild != null) {
                List<SlashCommandData> allCommands = commandRegistry.values().stream()
                        .map(SlashCommand::buildCommandData)
                        .toList();
                logger.info("Registering {} commands to dev guild: {} ({})", allCommands.size(), devGuild.getName(), devGuildId);
                devGuild.updateCommands().addCommands(allCommands).queue(
                        success -> logger.info("Successfully updated dev guild slash commands."),
                        failure -> logger.error("Failed to update dev guild slash commands", failure)
                );
                return;
            } else {
                logger.warn("Dev guild ID '{}' was configured, but guild was not found. Falling back to global registration.", devGuildId);
            }
        }

        List<SlashCommandData> globalCommands = commandRegistry.values().stream()
                .filter(cmd -> !cmd.isHomeGuildOnly())
                .map(SlashCommand::buildCommandData)
                .toList();
        List<SlashCommandData> homeCommands = commandRegistry.values().stream()
                .filter(SlashCommand::isHomeGuildOnly)
                .map(SlashCommand::buildCommandData)
                .toList();

        logger.info("Registering {} global slash commands with Discord...", globalCommands.size());
        event.getJDA().updateCommands().addCommands(globalCommands).queue(
                success -> logger.info("Successfully updated global slash commands."),
                failure -> logger.error("Failed to update global slash commands", failure)
        );

        String homeGuildId = homeGuildId();
        Guild homeGuild = homeGuildId.isBlank() ? null : event.getJDA().getGuildById(homeGuildId);
        if (homeGuild == null) {
            logger.warn("Home guild '{}' is not configured or not found. {} home-server commands were not registered.", homeGuildId, homeCommands.size());
            return;
        }

        logger.info("Registering {} home-server commands to {} ({})", homeCommands.size(), homeGuild.getName(), homeGuildId);
        homeGuild.updateCommands().addCommands(homeCommands).queue(
                success -> logger.info("Successfully updated home guild slash commands."),
                failure -> logger.error("Failed to update home guild slash commands", failure)
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String guildId = event.getGuild() != null ? event.getGuild().getId() : null;

        if (blocklistService.isBlocked(userId, guildId)) {
            logger.warn("Blocked interaction attempt from user {} in guild {}", userId, guildId);
            event.reply("You or this server are blocked from using FashionScape Bot commands.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String commandName = event.getName();
        SlashCommand command = commandRegistry.get(commandName.toLowerCase(Locale.ROOT));

        if (command == null) {
            logger.warn("Unknown slash command invoked: /{}", commandName);
            event.reply("Unknown command: /" + commandName)
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (command.isHomeGuildOnly() && !isHomeOrDevGuild(guildId)) {
            logger.warn("Refused home-server command /{} from user {} in guild {}", commandName, userId, guildId);
            event.reply("This command can only be used in the FashionScape home server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        try {
            logger.debug("Executing slash command /{} for user {}", commandName, userId);
            command.execute(event);
        } catch (Exception e) {
            logger.error("Error executing slash command /{}", commandName, e);
            if (event.isAcknowledged()) {
                event.getHook().sendMessage("An unexpected error occurred while processing this command.")
                        .setEphemeral(true)
                        .queue();
            } else {
                event.reply("An unexpected error occurred while processing this command.")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        logger.debug("Received button interaction: {}", buttonId);

        String userId = event.getUser().getId();
        String guildId = event.getGuild() != null ? event.getGuild().getId() : null;

        if (blocklistService.isBlocked(userId, guildId)) {
            logger.warn("Blocked button interaction attempt from user {} in guild {}", userId, guildId);
            event.reply("You or this server are blocked from using FashionScape Bot.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        for (ButtonHandler handler : buttonHandlers) {
            if (handler.canHandle(buttonId)) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    logger.error("Error handling button interaction: {}", buttonId, e);
                    if (!event.isAcknowledged()) {
                        event.reply("Failed to process button interaction.")
                                .setEphemeral(true)
                                .queue();
                    }
                }
                return;
            }
        }

        logger.warn("No handler found for button: {}", buttonId);
    }

    private String homeGuildId() {
        if (properties.discord() == null || properties.discord().homeGuildId() == null) {
            return "";
        }
        return properties.discord().homeGuildId();
    }

    private boolean isHomeOrDevGuild(String guildId) {
        if (guildId == null) {
            return false;
        }
        String devGuildId = properties.discord() != null ? properties.discord().devGuildId() : null;
        return guildId.equals(homeGuildId()) || guildId.equals(devGuildId);
    }
}
