package dev.salmonllama.fsbot.utilities.warnings;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;

public class Warning {
    private static Color embedColor = Color.YELLOW;
    private static String embedFooter = "Bother Crablet#9999 to add usages to these things";
    private String embedWarning;

    public Warning(String warnMessage) {
        this.embedWarning = warnMessage;
    }

    public EmbedBuilder sendWarning() {
        EmbedBuilder warning = new EmbedBuilder()
                .setColor(embedColor)
                .setFooter(embedFooter)
                .addField("WARNING", embedWarning);

        return warning;
    }
}
