package dev.salmonllama.fsbot.database.models;

import dev.salmonllama.fsbot.database.DatabaseModel;

import java.sql.Timestamp;

public class UserBlacklist extends DatabaseModel {
    private final String id;
    private final String reason;
    private Timestamp added;

    private UserBlacklist(Builder builder) {
        this.id = builder.id;
        this.reason = builder.reason;
        this.added = builder.added;
    }

    public String getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public Timestamp getAdded() {
        return added;
    }

    public void setAdded(Timestamp added) {
        this.added = added;
    }

    public static String schema() {
        return "CREATE TABLE IF NOT EXISTS blacklist_users (" +
                "id TEXT PRIMARY KEY," +
                "reason TEXT," +
                "added TEXT)";
    }

    @Override
    public String toString() {
        return String.format("User Blacklist: [id: %s, reason: %s, added: %s]",
                this.getId(),
                this.getReason(),
                this.getAdded()
        );
    }

    public static class Builder {
        private String id;
        private String reason;
        private Timestamp added;

        public Builder(String id) {
            this.id = id;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder setAdded(Timestamp added) {
            this.added = added;
            return this;
        }

        public UserBlacklist build() {
            return new UserBlacklist(this);
        }
    }
}
