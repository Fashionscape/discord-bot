package dev.salmonllama.fsbot.discord.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface SlashCommand {

    /**
     * Name of the slash command (must be lowercase alphanumeric).
     */
    String getName();

    /**
     * Description of the slash command.
     */
    String getDescription();

    /**
     * Builds JDA SlashCommandData (options, subcommands, etc.) for Discord registration.
     */
    SlashCommandData buildCommandData();

    /**
     * Executes the slash command interaction.
     */
    void execute(SlashCommandInteractionEvent event);
}
