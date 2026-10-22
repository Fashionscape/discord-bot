package dev.salmonllama.fsbot;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.discord.commands.SlashCommandDispatcher;
import dev.salmonllama.fsbot.discord.listeners.GalleryEventListener;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class DiscordBotRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DiscordBotRunner.class);

    private final BotProperties properties;
    private final SlashCommandDispatcher slashCommandDispatcher;
    private final GalleryEventListener galleryEventListener;

    private JDA jda;
    private ExecutorService virtualExecutor;
    private ScheduledExecutorService virtualScheduledExecutor;

    public DiscordBotRunner(
            BotProperties properties,
            SlashCommandDispatcher slashCommandDispatcher,
            GalleryEventListener galleryEventListener
    ) {
        this.properties = properties;
        this.slashCommandDispatcher = slashCommandDispatcher;
        this.galleryEventListener = galleryEventListener;
    }

    @Override
    public void run(String... args) throws Exception {
        BotProperties.DiscordProperties discordProps = properties.discord();
        String token = discordProps != null ? discordProps.token() : null;

        if (token != null && !token.isBlank() && !token.contains("here")) {
            logger.info("Initializing JDA 5 with Java 21 Virtual Thread pools...");

            this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
            this.virtualScheduledExecutor = Executors.newScheduledThreadPool(2, Thread.ofVirtual().factory());

            this.jda = JDABuilder.createDefault(token)
                    .setEventManager(new InterfacedEventManager())
                    .setEventPassthrough(true)
                    .setCallbackPool(virtualExecutor)
                    .setEventPool(virtualExecutor)
                    .setRateLimitElastic(virtualExecutor)
                    .setGatewayPool(virtualScheduledExecutor)
                    .setRateLimitScheduler(virtualScheduledExecutor)
                    .enableIntents(
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MEMBERS
                    )
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .addEventListeners(slashCommandDispatcher, galleryEventListener)
                    .build();

            logger.info("JDA 5 initialization initiated.");
        } else {
            logger.warn("No valid Discord token configured. Skipping JDA gateway login.");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            logger.info("Shutting down JDA...");
            jda.shutdown();
        }
        if (virtualExecutor != null) {
            virtualExecutor.shutdown();
        }
        if (virtualScheduledExecutor != null) {
            virtualScheduledExecutor.shutdown();
        }
    }

    public JDA getJda() {
        return jda;
    }
}
