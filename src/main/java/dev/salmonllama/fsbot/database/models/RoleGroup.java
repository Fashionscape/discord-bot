package dev.salmonllama.fsbot.database.models;

import dev.salmonllama.fsbot.database.DatabaseModel;
import dev.salmonllama.fsbot.services.IdService;

public class RoleGroup extends DatabaseModel {
    public final String id;
    public String messageId;
    public String name;
    public String requiredRole;

    public RoleGroup() {
        this.id = IdService.generateShort();
    }
}
