package dev.salmonllama.fsbot.guthix;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class Command {
    public abstract String name();
    public abstract String description();
    public abstract String usage();
    public abstract CommandCategory category();
    public abstract CommandPermission permission();
    public abstract List<String> aliases();

    public abstract void onCommand(CommandContext ctx);

    public void invoke(final CommandContext ctx) {
        CompletableFuture.runAsync(() -> onCommand(ctx));
    }
}
