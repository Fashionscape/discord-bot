package dev.salmonllama.fsbot.discord.components;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonHandler {

    /**
     * Determines whether this handler can process the given button component ID.
     */
    boolean canHandle(String buttonId);

    /**
     * Handles the button interaction event.
     */
    void handle(ButtonInteractionEvent event);
}
