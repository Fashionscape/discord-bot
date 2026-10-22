package dev.salmonllama.fsbot.discord.commands.general;

import dev.salmonllama.fsbot.discord.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.temporal.ChronoUnit;

@Component
public class PingSlashCommand implements SlashCommand {

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getDescription() {
        return "Check bot latency and gateway connection status";
    }

    @Override
    public SlashCommandData buildCommandData() {
        return Commands.slash(getName(), getDescription());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();

        event.deferReply().queue(hook -> {
            long restPing = event.getTimeCreated().until(hook.getInteraction().getTimeCreated(), ChronoUnit.MILLIS);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Pong!")
                    .setColor(new Color(0x3B88C3))
                    .addField("Gateway Latency", gatewayPing + " ms", true)
                    .addField("REST Latency", Math.abs(restPing) + " ms", true)
                    .setFooter("FashionScape Bot v3 (Java 21 / JDA 5)");

            hook.sendMessageEmbeds(embed.build()).queue();
        });
    }
}
