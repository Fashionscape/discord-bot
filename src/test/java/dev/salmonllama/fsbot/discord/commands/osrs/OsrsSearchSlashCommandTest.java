package dev.salmonllama.fsbot.discord.commands.osrs;

import dev.salmonllama.fsbot.endpoints.scapefashion.*;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OsrsSearchSlashCommandTest {

    @Mock
    private ScapeFashionClient scapeFashionClient;

    @Mock
    private SlashCommandInteractionEvent slashEvent;

    @Mock
    private ReplyCallbackAction replyCallbackAction;

    @Mock
    private InteractionHook interactionHook;

    @Mock
    private WebhookMessageCreateAction<net.dv8tion.jda.api.entities.Message> webhookMessageAction;

    private OsrsSearchSlashCommand command;

    @BeforeEach
    void setUp() {
        command = new OsrsSearchSlashCommand(scapeFashionClient);

        when(slashEvent.deferReply()).thenReturn(replyCallbackAction);
        doAnswer(inv -> {
            Consumer<InteractionHook> callback = inv.getArgument(0);
            callback.accept(interactionHook);
            return null;
        }).when(replyCallbackAction).queue(any());

        when(interactionHook.sendMessage(anyString())).thenReturn(webhookMessageAction);
        when(interactionHook.sendMessageEmbeds(any(MessageEmbed.class))).thenReturn(webhookMessageAction);
    }

    @Test
    void testCommandData() {
        assertEquals("osrssearch", command.getName());
        assertNotNull(command.getDescription());

        SlashCommandData data = command.buildCommandData();
        assertEquals("osrssearch", data.getName());
        assertEquals(2, data.getOptions().size());
    }

    @Test
    void testExecuteItemSearchSuccess() {
        when(slashEvent.getOption(eq("query"), any())).thenReturn("Abyssal whip");

        ScapeFashionItem item = new ScapeFashionItem.Builder()
                .setName("Abyssal whip")
                .setSlot("weapon")
                .setColors(new String[]{"#800000"})
                .setMatch(0.95f)
                .build();

        ScapeFashionResult result = new ScapeFashionResult("https://scape.fashion/items/whip");
        result.addItem(item);

        when(scapeFashionClient.searchOsrsItem(eq("Abyssal whip"), any()))
                .thenReturn(CompletableFuture.completedFuture(result));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(interactionHook).sendMessageEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();
        assertEquals("OSRS Best Match: Abyssal whip", embed.getTitle());
    }

    @Test
    void testExecuteColorSearchSuccess() {
        when(slashEvent.getOption(eq("query"), any())).thenReturn("#FF0000");

        ScapeFashionItem item = new ScapeFashionItem.Builder()
                .setName("Dragon med helm")
                .setSlot("head")
                .setColors(new String[]{"#FF0000"})
                .setMatch(0.91f)
                .build();

        ScapeFashionResult result = new ScapeFashionResult("https://scape.fashion/colors/%23FF0000");
        result.addItem(item);

        when(scapeFashionClient.searchOsrsColor(eq("#FF0000"), any()))
                .thenReturn(CompletableFuture.completedFuture(result));

        command.execute(slashEvent);

        verify(interactionHook).sendMessageEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testExecuteNoResults() {
        when(slashEvent.getOption(eq("query"), any())).thenReturn("NonexistentItemXYZ");

        ScapeFashionResult emptyResult = new ScapeFashionResult("https://scape.fashion");
        when(scapeFashionClient.searchOsrsItem(eq("NonexistentItemXYZ"), any()))
                .thenReturn(CompletableFuture.completedFuture(emptyResult));

        command.execute(slashEvent);

        verify(interactionHook).sendMessage(contains("No Old School RuneScape items found"));
    }
}
