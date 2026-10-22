package dev.salmonllama.fsbot.guthix;

public enum CommandCategory {
    DEVELOPER("Developer"),
    GENERAL("General"),
    OSRS_ITEM_SEARCH("07Search"),
    RS3_ITEM_SEARCH("RS3Search"),
    STAFF("Staff");

    private final String category;

    CommandCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
