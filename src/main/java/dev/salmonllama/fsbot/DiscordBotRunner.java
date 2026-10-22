package dev.salmonllama.fsbot;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.guthix.Guthix;
import dev.salmonllama.fsbot.listeners.ImageListener;
import dev.salmonllama.fsbot.listeners.ServerJoined;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DiscordBotRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DiscordBotRunner.class);

    private final BotProperties properties;

    public DiscordBotRunner(BotProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... args) throws Exception {
        BotProperties.DiscordProperties discordProps = properties.discord();
        String token = discordProps != null ? discordProps.token() : null;

        if (token != null && !token.isBlank() && !token.contains("here")) {
            FSDB.init();

            new DiscordApiBuilder().addIntents(Intent.MESSAGE_CONTENT).setToken(token).login().thenAccept(api -> {

                @SuppressWarnings("unused")
                Guthix guthix = new Guthix(api);

                // Register listeners
                api.addMessageCreateListener(new ImageListener());
                api.addServerJoinListener(new ServerJoined());

                logger.info("{} reporting for duty", api.getYourself().getDiscriminatedName());
            });
        } else {
            logger.warn("No valid Discord token configured. Skipping legacy Discord gateway login.");
        }
    }
}
