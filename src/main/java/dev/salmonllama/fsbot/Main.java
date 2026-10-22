/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.guthix.Guthix;
import dev.salmonllama.fsbot.listeners.*;
import org.javacord.api.DiscordApiBuilder;

import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Main implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    @Autowired
    BotProperties properties;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        FSDB.init();

        BotProperties.DiscordProperties discordProps = properties.discord();
        String token = discordProps.token();

        if (token != null && !token.isBlank() && !token.contains("here")) {
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
