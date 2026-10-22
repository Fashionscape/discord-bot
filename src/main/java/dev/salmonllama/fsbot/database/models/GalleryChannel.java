package dev.salmonllama.fsbot.database.models;

import dev.salmonllama.fsbot.database.DatabaseModel;

public class GalleryChannel extends DatabaseModel {
    private final String serverId;
    private final String channelId;
    private final String tag;
    // Normal emojis will be stored in plain text as :emoji:
    // Server emojis will be stored in plain text as <:emoji:emoji-id>
    // This can be acquired through CustomEmoji#getMentionTag()
    private final String emoji;

    public GalleryChannel(GalleryBuilder builder) {
        serverId = builder.serverId;
        channelId = builder.channelId;
        tag = builder.tag;
        emoji = builder.emoji;
    }

    public String getServerId() {
        return serverId;
    }

    public String getChannelId() {
        return channelId;
    }


    public String getTag() {
        return tag;
    }

    public String getEmoji() {
        return emoji;
    }

    public static String schema() {
        return "CREATE TABLE IF NOT EXISTS galleries (" +
                "server_id TEXT," +
                "channel_id TEXT," +
                "emoji TEXT," +
                "tag TEXT)";
    }

    @Override
    public String toString() {
        return String.format("Gallery: [server id: %s, channel id: %s, tag: %s, emoji: %s]",
                serverId, channelId, tag, emoji);
    }

    public static class GalleryBuilder {
        private String serverId;
        private String channelId;
        private String tag;
        private String emoji;

        public GalleryBuilder() {

        }
        public GalleryBuilder setServerId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public GalleryBuilder setChannelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public GalleryBuilder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public GalleryBuilder setEmoji(String emoji) {
            this.emoji = emoji;
            return this;
        }

        public GalleryChannel build() {
            return new GalleryChannel(this);
        }
    }
}
