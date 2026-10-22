package dev.salmonllama.fsbot.discord.commands;

import dev.salmonllama.fsbot.config.BotProperties;
import dev.salmonllama.fsbot.discord.components.ButtonHandler;
import dev.salmonllama.fsbot.services.BlocklistService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlashCommandDispatcherTest {

    @Mock
    private BlocklistService blocklistService;

    @Mock
    private SlashCommand mockCommand;

    @Mock
    private ButtonHandler mockButtonHandler;

    @Mock
    private SlashCommandInteractionEvent slashEvent;

    @Mock
    private ButtonInteractionEvent buttonEvent;

    @Mock
    private User mockUser;

    @Mock
    private Guild mockGuild;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    @Mock
    private InteractionHook interactionHook;

    private BotProperties properties;
    private SlashCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(mockCommand.getName()).thenReturn("testcmd");
        lenient().when(mockCommand.buildCommandData()).thenReturn(Commands.slash("testcmd", "A test command"));

        properties = new BotProperties(
                new BotProperties.DiscordProperties("test-token", 123456789L, ""),
                new BotProperties.MinioProperties("http://localhost:9000", "key", "secret", "bucket", "https://cdn.example.com")
        );

        dispatcher = new SlashCommandDispatcher(
                List.of(mockCommand),
                List.of(mockButtonHandler),
                blocklistService,
                properties
        );
    }

    @Test
    void testRegistryLookup() {
        assertTrue(dispatcher.getCommand("testcmd").isPresent());
        assertTrue(dispatcher.getCommand("TESTCMD").isPresent());
        assertFalse(dispatcher.getCommand("unknown").isPresent());
        assertEquals(1, dispatcher.getRegisteredCommands().size());
    }

    @Test
    void testSuccessfulExecution() {
        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("user-1");
        when(slashEvent.getGuild()).thenReturn(mockGuild);
        when(mockGuild.getId()).thenReturn("guild-1");
        when(slashEvent.getName()).thenReturn("testcmd");

        when(blocklistService.isBlocked("user-1", "guild-1")).thenReturn(false);

        dispatcher.onSlashCommandInteraction(slashEvent);

        verify(mockCommand).execute(slashEvent);
    }

    @Test
    void testBlockedUserInteraction() {
        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("blocked-user");
        when(slashEvent.getGuild()).thenReturn(mockGuild);
        when(mockGuild.getId()).thenReturn("guild-1");

        when(blocklistService.isBlocked("blocked-user", "guild-1")).thenReturn(true);
        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);

        dispatcher.onSlashCommandInteraction(slashEvent);

        verify(slashEvent).reply(contains("blocked"));
        verify(mockCommand, never()).execute(any());
    }

    @Test
    void testUnknownCommandInteraction() {
        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("user-1");
        when(slashEvent.getGuild()).thenReturn(null);
        when(slashEvent.getName()).thenReturn("notfound");

        when(blocklistService.isBlocked("user-1", null)).thenReturn(false);
        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);

        dispatcher.onSlashCommandInteraction(slashEvent);

        verify(slashEvent).reply(contains("Unknown command"));
        verify(mockCommand, never()).execute(any());
    }

    @Test
    void testCommandExceptionHandling() {
        when(slashEvent.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn("user-1");
        when(slashEvent.getGuild()).thenReturn(null);
        when(slashEvent.getName()).thenReturn("testcmd");
        when(blocklistService.isBlocked("user-1", null)).thenReturn(false);

        doThrow(new RuntimeException("Simulated error")).when(mockCommand).execute(slashEvent);
        when(slashEvent.isAcknowledged()).thenReturn(false);
        when(slashEvent.reply(anyString())).thenReturn(replyCallbackAction);
        when(replyCallbackAction.setEphemeral(true)).thenReturn(replyCallbackAction);

        dispatcher.onSlashCommandInteraction(slashEvent);

        verify(slashEvent).reply(contains("unexpected error"));
    }

    @Test
    void testButtonDispatching() {
        when(buttonEvent.getComponentId()).thenReturn("gallery:upvote:outfit1");
        when(mockButtonHandler.canHandle("gallery:upvote:outfit1")).thenReturn(true);

        dispatcher.onButtonInteraction(buttonEvent);

        verify(mockButtonHandler).handle(buttonEvent);
    }

    @Test
    void testOnReadyGlobalRegistration() {
        ReadyEvent readyEvent = mock(ReadyEvent.class);
        JDA jda = mock(JDA.class);
        SelfUser selfUser = mock(SelfUser.class);
        CommandListUpdateAction updateAction = mock(CommandListUpdateAction.class);

        when(readyEvent.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(selfUser.getAsTag()).thenReturn("TestBot#0001");
        when(jda.updateCommands()).thenReturn(updateAction);
        when(updateAction.addCommands(anyList())).thenReturn(updateAction);

        dispatcher.onReady(readyEvent);

        verify(jda).updateCommands();
        verify(updateAction).queue(any(), any());
    }
}
