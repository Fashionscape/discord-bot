package dev.salmonllama.fsbot.discord.commands.general;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PingSlashCommandTest {

    private PingSlashCommand pingCommand;

    @Mock
    private SlashCommandInteractionEvent event;

    @Mock
    private JDA jda;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    @Mock
    private InteractionHook interactionHook;

    @BeforeEach
    void setUp() {
        pingCommand = new PingSlashCommand();
    }

    @Test
    void testMetadata() {
        assertEquals("ping", pingCommand.getName());
        assertNotNull(pingCommand.getDescription());
        SlashCommandData data = pingCommand.buildCommandData();
        assertEquals("ping", data.getName());
        assertEquals(pingCommand.getDescription(), data.getDescription());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecution() {
        when(event.getJDA()).thenReturn(jda);
        when(jda.getGatewayPing()).thenReturn(42L);
        when(event.getTimeCreated()).thenReturn(OffsetDateTime.now());
        when(event.deferReply()).thenReturn(replyCallbackAction);

        net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction interaction =
                mock(net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction.class);
        when(interaction.getTimeCreated()).thenReturn(OffsetDateTime.now().plusNanos(50_000_000));
        when(interactionHook.getInteraction()).thenReturn(interaction);
        when(interactionHook.sendMessageEmbeds(any(net.dv8tion.jda.api.entities.MessageEmbed.class)))
                .thenReturn(mock(net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction.class));

        doAnswer(invocation -> {
            Consumer<InteractionHook> consumer = invocation.getArgument(0);
            consumer.accept(interactionHook);
            return null;
        }).when(replyCallbackAction).queue(any(Consumer.class));

        pingCommand.execute(event);

        verify(event).deferReply();
        verify(interactionHook).sendMessageEmbeds(any(net.dv8tion.jda.api.entities.MessageEmbed.class));
    }
}
