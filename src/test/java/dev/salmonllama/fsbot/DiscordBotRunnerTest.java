package dev.salmonllama.fsbot;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.discord.commands.SlashCommandDispatcher;
import dev.salmonllama.fsbot.discord.listeners.GalleryEventListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DiscordBotRunnerTest {

    @Mock
    private SlashCommandDispatcher slashCommandDispatcher;

    @Mock
    private GalleryEventListener galleryEventListener;

    @Test
    void testRunWithoutTokenSkipsJDA() throws Exception {
        BotProperties properties = new BotProperties(
                new BotProperties.DiscordProperties("", 0L, "", ""),
                new BotProperties.MinioProperties("http://localhost:9000", "key", "secret", "bucket", "https://cdn.example.com")
        );

        DiscordBotRunner runner = new DiscordBotRunner(properties, slashCommandDispatcher, galleryEventListener);
        runner.run();

        assertNull(runner.getJda());
        assertDoesNotThrow(runner::shutdown);
    }
}
