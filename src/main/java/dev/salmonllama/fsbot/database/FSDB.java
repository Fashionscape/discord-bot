// Heavily inspired by Kaaz's Emily database connection: https://github.com/Kaaz/DiscordBot/tree/master/src/main/java/emily/db
package dev.salmonllama.fsbot.database;

import dev.salmonllama.fsbot.database.models.*;

import java.sql.SQLException;
import java.util.HashMap;

public class FSDB {
    private static final String DEFAULT_CONNECTION = "fsbot";
    private static final HashMap<String, DatabaseProvider> connections = new HashMap<>();

    public static DatabaseProvider get(String key) {
        if (connections.containsKey(key)) {
            return connections.get(key);
        }

        System.out.printf("Specified connection %s has not been set.%n", key);
        return null;
    }

    public static DatabaseProvider get() {
        return connections.get(DEFAULT_CONNECTION);
    }

    public static void init() {
        connections.clear();
        connections.put(DEFAULT_CONNECTION, new DatabaseProvider("This is going away soon"));

        prepareTables();
    }

    private static void prepareTables() {
        try {
            get().query(Outfit.schema());
//            get().query(ColorRole.schema());
            get().query(GalleryChannel.schema());
            get().query(ServerConfig.schema());
            get().query(ServerBlacklist.schema());
            get().query(UserBlacklist.schema());
            get().query(StaticPermission.schema());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
