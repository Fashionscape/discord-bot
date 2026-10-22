package dev.salmonllama.fsbot.database.models;

import dev.salmonllama.fsbot.database.DatabaseModel;

public class Role extends DatabaseModel {
    public String roleId;
    public String serverId;
    public String color;
    public String groupId;

    public Role(
            String roleId,
            String serverId,
            String color,
            String groupId
    ) {
        this.roleId = roleId;
        this.serverId = serverId;
        this.color = color;
        this.groupId = groupId;
    }

    public static String schema() {
        return "CREATE TABLE IF NOT EXISTS color_roles (role_id TEXT, server_id TEXT, color TEXT, group_id TEXT)";
    }

    @Override
    public String toString() {
        return String.format("Color Role: {roleId: %s, serverId: %s, color: %s, groupId: %s}", roleId, serverId, color, groupId);
    }
}
