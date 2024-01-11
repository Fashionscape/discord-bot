/*
 * Copyright (c) 2021 Aleksei Gryczewski
 */

package dev.salmonllama.fsbot;

import dev.salmonllama.fsbot.config.BotConfig;
// import dev.salmonllama.fsbot.config.SecretManager;
import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.guthix.Guthix;
import dev.salmonllama.fsbot.listeners.*;
import org.javacord.api.DiscordApiBuilder;

import dev.salmonllama.fsbot.utilities.Constants;
import org.javacord.api.entity.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        BotConfig.initConfig(Constants.BOT_FOLDER, false);

        FSDB.init();

        String token;
        if (System.getenv("ENVIRONMENT") != null)
        {
            token = BotConfig.BOT_TOKEN_PROD;
            // token = SecretManager.DISCORD_TOKEN_PROD.getPlainText();
        }
        else
        {
            token = BotConfig.BOT_TOKEN_DEV;
            // token = SecretManager.DISCORD_TOKEN.getPlainText();
        }

        new DiscordApiBuilder().addIntents(Intent.MESSAGE_CONTENT).setToken(token).login().thenAccept(api -> {

            @SuppressWarnings("unused")
            Guthix guthix = new Guthix(api);

            // Register listeners
            api.addMessageCreateListener(new ImageListener());
            api.addServerMemberJoinListener(new NewMemberListener());
            api.addServerJoinListener(new ServerJoined());
            api.addMessageCreateListener(new ThumbsListener());
            api.addMessageCreateListener(new AchievementListener());
            api.addMessageCreateListener(new ReportListener());

            logger.info("{} reporting for duty", api.getYourself().getDiscriminatedName());
        });

        SpringApplication.run(Main.class, args);
    }
}
