package dev.salmonllama.fsbot.endpoints.scapefashion;

import java.util.Optional;

public class ScapeFashionConnection {

    private final ScapeFashionClient client;

    public ScapeFashionConnection() {
        this.client = new ScapeFashionClient();
    }

    public ScapeFashionConnection(ScapeFashionClient client) {
        this.client = client != null ? client : new ScapeFashionClient();
    }

    public ScapeFashionResult osrsColor(String color) throws Exception {
        return client.searchOsrsColor(color, Optional.empty()).join();
    }

    public ScapeFashionResult osrsColor(String color, ScapeFashionSlotOsrs slot) throws Exception {
        return client.searchOsrsColor(color, Optional.ofNullable(slot)).join();
    }

    public ScapeFashionResult osrsItem(String item) throws Exception {
        return client.searchOsrsItem(item, Optional.empty()).join();
    }

    public ScapeFashionResult osrsItem(String item, ScapeFashionSlotOsrs slot) throws Exception {
        return client.searchOsrsItem(item, Optional.ofNullable(slot)).join();
    }

    public ScapeFashionResult rs3Color(String color) throws Exception {
        return client.searchRs3Color(color, Optional.empty()).join();
    }

    public ScapeFashionResult rs3Color(String color, ScapeFashionSlotRs3 slot) throws Exception {
        return client.searchRs3Color(color, Optional.ofNullable(slot)).join();
    }

    public ScapeFashionResult rs3Item(String item) throws Exception {
        return client.searchRs3Item(item, Optional.empty()).join();
    }

    public ScapeFashionResult rs3Item(String item, ScapeFashionSlotRs3 slot) throws Exception {
        return client.searchRs3Item(item, Optional.ofNullable(slot)).join();
    }
}
