package dev.salmonllama.fsbot.utilities.exceptions;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;

public class DiscordError {

    String title = "Error!";
    String message;
    String footer = "Contact Salmonllama#5727 if you think this is wrong";

    public DiscordError(String message) {
        this.message = message;
    }

    public EmbedBuilder get() {
        return new EmbedBuilder().setTitle(this.title).setDescription(this.message).setColor(Color.RED).setFooter(this.footer);
    }
}
