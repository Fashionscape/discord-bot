package dev.salmonllama.fsbot.listeners;

import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.listener.server.ServerJoinListener;

public class ServerJoined implements ServerJoinListener {

    public void onServerJoin(ServerJoinEvent event) {
    //     db.newServerProcess(event.getServer());

    //     EmbedBuilder embed = new EmbedBuilder()
    //             .setTitle("Server joined")
    //             .setColor(Color.GREEN)
    //             .addInlineField("Server name:", event.getServer().getName())
    //             .addInlineField("Server Id:", event.getServer().getIdAsString());
    }
}
