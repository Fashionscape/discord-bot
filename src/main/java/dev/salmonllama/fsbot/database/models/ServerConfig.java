package dev.salmonllama.fsbot.database.models;

import dev.salmonllama.fsbot.database.DatabaseModel;

public class ServerConfig extends DatabaseModel {
    private final String id;
    private final String prefix;
    private final String welcomeMessage;
    private final String welcomeChannel;

    public ServerConfig(ServerConfigBuilder builder) {
        this.id = builder.id;
        this.prefix = builder.prefix;
        this.welcomeMessage = builder.welcomeMessage;
        this.welcomeChannel = builder.welcomeChannel;
    }

    public String getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public String getWelcomeChannel() {
        return welcomeChannel;
    }

    public static String schema() {
        return "CREATE TABLE IF NOT EXISTS server_config (" +
                "id TEXT," +
                "prefix TEXT," +
                "welcome_message TEXT," +
                "welcome_channel TEXT)";
    }

    @Override
    public String toString() {
        return String.format("Server Config: [id: %s, prefix: %s, welcome_message: %s, welcome_channel: %s]",
                id, prefix, welcomeMessage, welcomeChannel
        );
    }

    public static class ServerConfigBuilder {
        private String id;
        private String prefix;
        private String welcomeMessage;
        private String welcomeChannel;

        public ServerConfigBuilder() {

        }

        public ServerConfigBuilder from(ServerConfig config) {
            this.id = config.id;
            this.prefix = config.prefix;
            this.welcomeMessage = config.welcomeMessage;
            this.welcomeChannel = config.welcomeChannel;
            return this;
        }

        public ServerConfigBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public ServerConfigBuilder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ServerConfigBuilder setWelcomeMessage(String welcomeMessage) {
            this.welcomeMessage = welcomeMessage;
            return this;
        }

        public ServerConfigBuilder setWelcomeChannel(String welcomeChannel) {
            this.welcomeChannel = welcomeChannel;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }
}