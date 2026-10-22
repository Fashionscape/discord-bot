package dev.salmonllama.fsbot.endpoints.scapefashion;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ScapeFashionClient {

    private static final Logger logger = LoggerFactory.getLogger(ScapeFashionClient.class);

    private static final String RS3_REQUEST_URL = "https://api.rune.scape.fashion";
    private static final String RS3_LINK_URL = "https://rune.scape.fashion";

    private static final String OSRS_REQUEST_URL = "https://api.scape.fashion";
    private static final String OSRS_LINK_URL = "https://scape.fashion";

    private static final String USER_AGENT = "FashionScape-Bot/3.0 (github.com/salmonllama/fashionscape-bot)";

    private final HttpClient httpClient;
    private final Gson gson;

    public ScapeFashionClient() {
        this(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build(), new Gson());
    }

    public ScapeFashionClient(HttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson != null ? gson : new Gson();
    }

    public CompletableFuture<ScapeFashionResult> searchOsrsColor(String color, Optional<ScapeFashionSlotOsrs> slot) {
        String encodedColor = encode(color);
        StringBuilder uriBuilder = new StringBuilder(OSRS_REQUEST_URL).append("/colors/").append(encodedColor);
        StringBuilder linkBuilder = new StringBuilder(OSRS_LINK_URL).append("/colors/").append(encodedColor);

        slot.ifPresent(s -> {
            String encodedSlot = encode(s.getValue());
            uriBuilder.append("?slot=").append(encodedSlot);
            linkBuilder.append("?slot=").append(encodedSlot);
        });

        String link = linkBuilder.toString();
        return executeRequest(uriBuilder.toString(), link, null);
    }

    public CompletableFuture<ScapeFashionResult> searchOsrsItem(String item, Optional<ScapeFashionSlotOsrs> slot) {
        String encodedItem = encode(item);
        StringBuilder uriBuilder = new StringBuilder(OSRS_REQUEST_URL).append("/items/").append(encodedItem);
        StringBuilder linkBuilder = new StringBuilder(OSRS_LINK_URL).append("/items/").append(encodedItem);

        slot.ifPresent(s -> {
            String encodedSlot = encode(s.getValue());
            uriBuilder.append("?slot=").append(encodedSlot);
            linkBuilder.append("?slot=").append(encodedSlot);
        });

        String link = linkBuilder.toString();
        return executeRequest(uriBuilder.toString(), link, item);
    }

    public CompletableFuture<ScapeFashionResult> searchRs3Color(String color, Optional<ScapeFashionSlotRs3> slot) {
        String encodedColor = encode(color);
        StringBuilder uriBuilder = new StringBuilder(RS3_REQUEST_URL).append("/colors/").append(encodedColor);
        StringBuilder linkBuilder = new StringBuilder(RS3_LINK_URL).append("/colors/").append(encodedColor);

        slot.ifPresent(s -> {
            String encodedSlot = encode(s.getValue());
            uriBuilder.append("?slot=").append(encodedSlot);
            linkBuilder.append("?slot=").append(encodedSlot);
        });

        String link = linkBuilder.toString();
        return executeRequest(uriBuilder.toString(), link, null);
    }

    public CompletableFuture<ScapeFashionResult> searchRs3Item(String item, Optional<ScapeFashionSlotRs3> slot) {
        String encodedItem = encode(item);
        StringBuilder uriBuilder = new StringBuilder(RS3_REQUEST_URL).append("/items/").append(encodedItem);
        StringBuilder linkBuilder = new StringBuilder(RS3_LINK_URL).append("/items/").append(encodedItem);

        slot.ifPresent(s -> {
            String encodedSlot = encode(s.getValue());
            uriBuilder.append("?slot=").append(encodedSlot);
            linkBuilder.append("?slot=").append(encodedSlot);
        });

        String link = linkBuilder.toString();
        return executeRequest(uriBuilder.toString(), link, item);
    }

    private CompletableFuture<ScapeFashionResult> executeRequest(String url, String webLink, String queryItemToRemove) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        logger.warn("ScapeFashion API returned status {} for URL: {}", response.statusCode(), url);
                        return new ScapeFashionResult(webLink);
                    }

                    String json = response.body();
                    ScapeFashionResult result;
                    try {
                        result = gson.fromJson(json, ScapeFashionResult.class);
                        if (result == null) {
                            result = new ScapeFashionResult(webLink);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse ScapeFashion JSON response", e);
                        result = new ScapeFashionResult(webLink);
                    }

                    result.setLink(webLink);

                    if (queryItemToRemove != null && result.hasItems()) {
                        String firstItemName = result.getItems().get(0).getName();
                        if (firstItemName != null && firstItemName.equalsIgnoreCase(queryItemToRemove)) {
                            result.getItems().remove(0);
                        }
                    }

                    return result;
                });
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
