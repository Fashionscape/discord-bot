package dev.salmonllama.fsbot.commands.rs3search;

import dev.salmonllama.fsbot.endpoints.scapefashion.ScapeFashionResult;
import org.apache.logging.log4j.util.Strings;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.Arrays;

public class Rs3SearchUtilities {
    static boolean isColor(String s) {
        return s.startsWith("#");
    }

    static void sendResult(ScapeFashionResult result, TextChannel channel) {
        var bestMatch = result.getItems().get(0);
        var colors = Strings.join(Arrays.asList(bestMatch.getColors()), ',');
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(String.format("Best Match - %s", bestMatch.getName()))
                .setImage(bestMatch.getImages().getDetail())
                .setAuthor(String.format("Wiki: %s", bestMatch.getName()), bestMatch.getWiki().getLink(), bestMatch.getImages().getDetail())
                .setUrl(result.getLink())
                .setColor(Color.decode(bestMatch.getColors()[0]))
                .addField("Match:", String.valueOf(bestMatch.getMatch()), true)
                .addField("Colors:", colors, true);

        channel.sendMessage(embed);
    }
}
