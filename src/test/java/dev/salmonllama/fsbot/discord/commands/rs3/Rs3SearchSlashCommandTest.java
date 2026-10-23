package dev.salmonllama.fsbot.discord.commands.rs3;

import dev.salmonllama.fsbot.endpoints.scapefashion.*;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Rs3SearchSlashCommandTest {

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

    private Rs3SearchSlashCommand command;

    @BeforeEach
    void setUp() {
        command = new Rs3SearchSlashCommand(scapeFashionClient);

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
        assertEquals("rs3search", command.getName());
        assertNotNull(command.getDescription());

        SlashCommandData data = command.buildCommandData();
        assertEquals("rs3search", data.getName());
        assertEquals(2, data.getOptions().size());
    }

    @Test
    void testExecuteItemSearchSuccess() {
        when(slashEvent.getOption(eq("query"), any())).thenReturn("Noxious scythe");

        ScapeFashionItem item = new ScapeFashionItem.Builder()
                .setName("Noxious scythe")
                .setSlot("two-handed")
                .setColors(new String[]{"#111111"})
                .setMatch(0.89f)
                .build();

        ScapeFashionResult result = new ScapeFashionResult("https://rune.scape.fashion/items/scythe");
        result.addItem(item);

        when(scapeFashionClient.searchRs3Item(eq("Noxious scythe"), any()))
                .thenReturn(CompletableFuture.completedFuture(result));

        command.execute(slashEvent);

        ArgumentCaptor<MessageEmbed> embedCaptor = ArgumentCaptor.forClass(MessageEmbed.class);
        verify(interactionHook).sendMessageEmbeds(embedCaptor.capture());
        MessageEmbed embed = embedCaptor.getValue();
        assertEquals("RS3 Best Match: Noxious scythe", embed.getTitle());
    }

    @Test
    void testExecuteColorSearchSuccess() {
        when(slashEvent.getOption(eq("query"), any())).thenReturn("#00FF00");

        ScapeFashionItem item = new ScapeFashionItem.Builder()
                .setName("Rune platebody")
                .setSlot("torso")
                .setColors(new String[]{"#00FF00"})
                .setMatch(0.85f)
                .build();

        ScapeFashionResult result = new ScapeFashionResult("https://rune.scape.fashion/colors/%2300FF00");
        result.addItem(item);

        when(scapeFashionClient.searchRs3Color(eq("#00FF00"), any()))
                .thenReturn(CompletableFuture.completedFuture(result));

        command.execute(slashEvent);

        verify(interactionHook).sendMessageEmbeds(any(MessageEmbed.class));
    }

    @Test
    void testExecuteNoResults() {
        when(slashEvent.getOption(eq("query"), any())).thenReturn("NonexistentItemXYZ");

        ScapeFashionResult emptyResult = new ScapeFashionResult("https://rune.scape.fashion");
        when(scapeFashionClient.searchRs3Item(eq("NonexistentItemXYZ"), any()))
                .thenReturn(CompletableFuture.completedFuture(emptyResult));

        command.execute(slashEvent);

        verify(interactionHook).sendMessage(contains("No RuneScape 3 items found"));
    }
}
