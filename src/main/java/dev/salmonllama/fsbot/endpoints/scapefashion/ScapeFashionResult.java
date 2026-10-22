package dev.salmonllama.fsbot.endpoints.scapefashion;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ScapeFashionResult {
    private String link;
    @SerializedName("items")
    private final List<ScapeFashionItem> items = new ArrayList<>();

    public ScapeFashionResult() {

    }

    public ScapeFashionResult(String link) {
        this.link = link;
    }

    public ScapeFashionResult addItem(ScapeFashionItem item) {
        this.items.add(item);
        return this;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public List<ScapeFashionItem> getItems() {
        return items;
    }
}
