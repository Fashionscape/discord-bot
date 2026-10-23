package dev.salmonllama.fsbot.endpoints.scapefashion;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScapeFashionClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ScapeFashionClient client;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        client = new ScapeFashionClient(httpClient, gson);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchOsrsColorSuccess() {
        String json = """
                {
                  "items": [
                    {
                      "name": "Abyssal whip",
                      "slot": "weapon",
                      "colors": ["#800000", "#101010"],
                      "match": 0.95,
                      "images": { "detail": "https://scape.fashion/images/whip.png" },
                      "wiki": { "link": "https://oldschool.runescape.wiki/w/Abyssal_whip" }
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ScapeFashionResult result = client.searchOsrsColor("#800000", Optional.of(ScapeFashionSlotOsrs.WEAPON)).join();

        assertNotNull(result);
        assertTrue(result.hasItems());
        assertEquals("https://scape.fashion/colors/%23800000?slot=weapon", result.getLink());

        ScapeFashionItem item = result.getBestMatch().orElseThrow();
        assertEquals("Abyssal whip", item.getName());
        assertEquals("weapon", item.getSlot());
        assertEquals(0.95f, item.getMatch(), 0.001);
        assertEquals("https://scape.fashion/images/whip.png", item.getImages().getDetail());
        assertEquals("https://oldschool.runescape.wiki/w/Abyssal_whip", item.getWiki().getLink());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchOsrsItemRemovesExactMatch() {
        String json = """
                {
                  "items": [
                    {
                      "name": "Dragon scimitar",
                      "slot": "weapon",
                      "colors": ["#FF0000"],
                      "match": 1.0
                    },
                    {
                      "name": "Dragon dagger",
                      "slot": "weapon",
                      "colors": ["#FF0000"],
                      "match": 0.92
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ScapeFashionResult result = client.searchOsrsItem("Dragon scimitar", Optional.empty()).join();

        assertNotNull(result);
        assertTrue(result.hasItems());
        assertEquals(1, result.getItems().size());
        assertEquals("Dragon dagger", result.getItems().get(0).getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchRs3ColorHandlesHttpError() {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ScapeFashionResult result = client.searchRs3Color("#00FF00", Optional.empty()).join();

        assertNotNull(result);
        assertFalse(result.hasItems());
        assertTrue(result.getBestMatch().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchRs3ItemSuccess() {
        String json = """
                {
                  "items": [
                    {
                      "name": "Noxious scythe",
                      "slot": "two-handed",
                      "colors": ["#1A1A1A"],
                      "match": 0.88
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ScapeFashionResult result = client.searchRs3Item("Noxious", Optional.of(ScapeFashionSlotRs3.TWO_HANDED)).join();

        assertNotNull(result);
        assertTrue(result.hasItems());
        assertEquals("Noxious scythe", result.getBestMatch().get().getName());
        assertEquals("https://rune.scape.fashion/items/Noxious?slot=two-handed", result.getLink());
    }
}
