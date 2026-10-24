package dev.salmonllama.fsbot.database.models;

import dev.salmonllama.fsbot.database.DatabaseModel;

import java.util.UUID;

public class RoleGroup extends DatabaseModel {
    public final String id;
    public String messageId;
    public String name;
    public String requiredRole;

    public RoleGroup() {
        this.id = UUID.randomUUID().toString();
    }
}
