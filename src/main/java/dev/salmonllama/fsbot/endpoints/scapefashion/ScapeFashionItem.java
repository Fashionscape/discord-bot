package dev.salmonllama.fsbot.endpoints.scapefashion;

public class ScapeFashionItem {
    private final ScapeFashionImages images;
    private final String name;
    private final String slot;
    private final ScapeFashionWiki wiki;
    private final String[] colors;
    private final float match;

    private ScapeFashionItem(Builder builder) {
        this.images = builder.images;
        this.name = builder.name;
        this.slot = builder.slot;
        this.wiki = builder.wiki;
        this.colors = builder.colors;
        this.match = builder.match;
    }

    public ScapeFashionImages getImages() {
        return images;
    }

    public String getName() {
        return name;
    }

    public String getSlot() {
        return slot;
    }

    public ScapeFashionWiki getWiki() {
        return wiki;
    }

    public String[] getColors() {
        return colors;
    }

    public float getMatch() {
        return match;
    }

    @Override
    public String toString() {
        return String.format("Item: [%s]", name);
    }

    public static class Builder {
        private ScapeFashionImages images;
        private String name;
        private String slot;
        private ScapeFashionWiki wiki;
        private String[] colors;
        private float match;

        public Builder() {

        }

        public Builder setImages(ScapeFashionImages images) {
            this.images = images;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSlot(String slot) {
            this.slot = slot;
            return this;
        }

        public Builder setWiki(ScapeFashionWiki wiki) {
            this.wiki = wiki;
            return this;
        }

        public Builder setColors(String[] colors) {
            this.colors = colors;
            return this;
        }

        public Builder setMatch(float match) {
            this.match = match;
            return this;
        }

        public ScapeFashionItem build() {
            return new ScapeFashionItem(this);
        }
    }
}
