package dev.salmonllama.fsbot.database.models;

import dev.salmonllama.fsbot.database.DatabaseModel;

import java.sql.Timestamp;

public class StaticPermission extends DatabaseModel {
    private String userId;
    private String permission;
    private Timestamp added;

    private StaticPermission(StaticPermissionBuilder builder) {
        userId = builder.userId;
        permission = builder.permission;
        added = builder.added;
    }

    public String getUserId() {
        return userId;
    }

    public String getPermission() {
        return permission;
    }

    public Timestamp getAdded() {
        return added;
    }

    public void setAdded(Timestamp added) {
        this.added = added;
    }

    public static String schema() {
        return "CREATE TABLE IF NOT EXISTS static_permissions (user_id TEXT, permission TEXT, date_added TEXT)";
    }

    @Override
    public String toString() {
        return String.format("Static Permission [userId: %s, permission: %s, added: %s", userId, permission, added.toString());
    }

    public static class StaticPermissionBuilder {
        private String userId;
        private String permission = null;
        private Timestamp added = null;

        public StaticPermissionBuilder(String userId) {
            this.userId = userId;
        }

        public StaticPermissionBuilder setPermission(String permission) {
            this.permission = permission;
            return this;
        }

        public StaticPermissionBuilder setAdded(Timestamp added) {
            this.added = added;
            return this;
        }

        public StaticPermission build() {
            return new StaticPermission(this);
        }
    }
}
