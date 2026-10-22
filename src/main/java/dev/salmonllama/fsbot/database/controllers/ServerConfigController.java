package dev.salmonllama.fsbot.database.controllers;

import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.database.models.ServerConfig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ServerConfigController {
    // insert
    // retrieve
    // update
    // No need for delete -> guilds should persist after leaving.

    public static CompletableFuture<Void> insert(ServerConfig config) {
        return CompletableFuture.runAsync(() -> {
            try {
                insertExec(config);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<ServerConfig>> get(String serverId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getExec(serverId);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> update(ServerConfig config) {
        return CompletableFuture.runAsync(() -> {
            try {
                updateExec(config);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static void insertExec(ServerConfig config) throws SQLException {
        FSDB.get().insert("INSERT INTO server_config(id, prefix, welcome_message) VALUES (?, ?, ?)",
                config.getId(),
                config.getPrefix(),
                config.getWelcomeMessage()
        );
    }

    private static Optional<ServerConfig> getExec(String serverId) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM server_config WHERE id = ?", serverId);

        if (rs.next()) {
            ServerConfig config = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(config);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static void updateExec(ServerConfig config) throws SQLException {
        FSDB.get().query("UPDATE server_config SET prefix = ?, welcome_message = ?, welcome_channel = ? WHERE id = ?",
                config.getPrefix(),
                config.getWelcomeMessage(),
                config.getWelcomeChannel(),
                config.getId()
                );
    }

    private static ServerConfig mapObject(ResultSet rs) throws SQLException {
        return new ServerConfig.ServerConfigBuilder()
                .setId(rs.getString("id"))
                .setPrefix(rs.getString("prefix"))
                .setWelcomeMessage(rs.getString("welcome_message"))
                .setWelcomeChannel(rs.getString("welcome_channel"))
                .build();
    }
}
