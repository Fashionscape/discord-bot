package dev.salmonllama.fsbot.discord.commands;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.database.repositories.BlockedServerRepository;
import dev.salmonllama.fsbot.database.repositories.BlockedUserRepository;
import dev.salmonllama.fsbot.discord.commands.general.PingSlashCommand;
import dev.salmonllama.fsbot.discord.components.ButtonHandler;
import dev.salmonllama.fsbot.services.BlocklistService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class SlashCommandDispatcherConcurrencyTest {

    @Autowired
    private SlashCommandDispatcher slashCommandDispatcher;

    @Autowired
    private List<SlashCommand> registeredCommands;

    @Autowired
    private BlocklistService blocklistService;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private BlockedServerRepository blockedServerRepository;

    @Autowired
    private BotProperties botProperties;

    @Test
    @DisplayName("Verify all expected slash commands are registered in SlashCommandDispatcher")
    void testAllExpectedCommandsAreRegistered() {
        assertNotNull(slashCommandDispatcher);
        assertFalse(registeredCommands.isEmpty());

        // Expected commands from Milestone 5
        List<String> expectedCommandNames = List.of("ping", "outfit", "block", "gallery", "osrssearch", "rs3search");

        for (String name : expectedCommandNames) {
            assertTrue(slashCommandDispatcher.getCommand(name).isPresent(),
                    "Command /" + name + " should be registered in SlashCommandDispatcher");
        }
    }

    @Test
    @DisplayName("Verify virtual thread concurrency: dispatch 100 concurrent slash interactions simultaneously")
    void testVirtualThreadConcurrency() throws InterruptedException {
        int concurrency = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicBoolean allRanOnVirtualThreads = new AtomicBoolean(true);

        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < concurrency; i++) {
                final int idx = i;
                virtualExecutor.submit(() -> {
                    try {
                        // Verify we are indeed executing on a Java 21 Virtual Thread
                        if (!Thread.currentThread().isVirtual()) {
                            allRanOnVirtualThreads.set(false);
                        }

                        // Wait for all threads to be ready before firing
                        startLatch.await();

                        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
                        User user = mock(User.class);
                        Guild guild = mock(Guild.class);
                        ReplyCallbackAction replyAction = mock(ReplyCallbackAction.class, RETURNS_DEEP_STUBS);

                        when(event.getUser()).thenReturn(user);
                        when(user.getId()).thenReturn("concurrent-user-" + idx);
                        when(event.getGuild()).thenReturn(guild);
                        when(guild.getId()).thenReturn("guild-concurrency");
                        when(event.getName()).thenReturn("ping");
                        when(event.reply(anyString())).thenReturn(replyAction);

                        slashCommandDispatcher.onSlashCommandInteraction(event);

                        successCounter.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Release all virtual threads at once
            startLatch.countDown();

            boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
            assertTrue(completed, "All 100 virtual threads should complete within timeout");
            assertTrue(allRanOnVirtualThreads.get(), "All tasks must execute on Java 21 Virtual Threads");
            assertEquals(concurrency, successCounter.get(), "All 100 interactions should succeed concurrently");
        }
    }

    @Test
    @DisplayName("Verify button interactions can be handled concurrently on virtual threads")
    void testConcurrentButtonInteractions() throws InterruptedException {
        int count = 50;
        CountDownLatch latch = new CountDownLatch(count);
        AtomicInteger handledCount = new AtomicInteger(0);

        ButtonHandler customHandler = new ButtonHandler() {
            @Override
            public boolean canHandle(String buttonId) {
                return buttonId != null && buttonId.startsWith("bench:");
            }

            @Override
            public void handle(ButtonInteractionEvent event) {
                handledCount.incrementAndGet();
            }
        };

        SlashCommandDispatcher customDispatcher = new SlashCommandDispatcher(
                List.of(),
                List.of(customHandler),
                blocklistService,
                botProperties
        );

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < count; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);
                        when(event.getComponentId()).thenReturn("bench:button:" + id);
                        customDispatcher.onButtonInteraction(event);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertTrue(completed);
            assertEquals(count, handledCount.get());
        }
    }
}
