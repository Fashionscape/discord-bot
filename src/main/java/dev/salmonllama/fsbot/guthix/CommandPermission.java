package dev.salmonllama.fsbot.guthix;

public class CommandPermission {
    private PermissionType type;
    private String value;

    public CommandPermission(PermissionType type) {
        this.type = type;
    }

    public CommandPermission(PermissionType type, String value) {
        this.type = type;
        this.value = value;
    }

    public PermissionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Command Permission: [PermissionType: %s", type.toString()));

        if (this.value != null) {
            builder.append(String.format(", Value: %s", value));
        }

        builder.append("]");
        return builder.toString();
    }
}
