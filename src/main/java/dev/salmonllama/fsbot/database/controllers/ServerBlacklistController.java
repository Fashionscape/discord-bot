package dev.salmonllama.fsbot.database.controllers;

import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.database.models.ServerBlacklist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ServerBlacklistController {
    // Need to create, read, and delete. No need for update?
    public static CompletableFuture<Void> insert(ServerBlacklist bl) {
        return CompletableFuture.runAsync(() -> {
            try {
                insertExec(bl);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Boolean> exists(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return existsExec(id);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Boolean> exists(ServerBlacklist bl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return existsExec(bl.getId());
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<ServerBlacklist>> get(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getExec(id);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteExec(id);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> delete(ServerBlacklist bl) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteExec(bl.getId());
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static void insertExec(ServerBlacklist bl) throws SQLException {
        // TODO: Check for null added timestamp and init
        FSDB.get().insert("INSERT INTO blacklist_servers ('id', 'name', 'owner_id', 'added') VALUES (?, ?, ?, ?)",
                bl.getId(),
                bl.getName(),
                bl.getOwnerId(),
                bl.getAdded()
        );
    }

    private static boolean existsExec(String id) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT EXISTS(SELECT 1 FROM blacklist_servers WHERE id = ?) AS hmm", id);
        boolean exists = rs.getBoolean("hmm");

        FSDB.get().close(rs);
        return exists;
    }

    private static Optional<ServerBlacklist> getExec(String id) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM blacklist_servers WHERE id = ?", id);

        if (rs.next()) {
            ServerBlacklist bl = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(bl);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static void deleteExec(String id) throws SQLException {
        FSDB.get().query("DELETE FROM blacklist_servers WHERE id = ?", id);
    }

    private static ServerBlacklist mapObject(ResultSet rs) throws SQLException {
        return new ServerBlacklist.ServerBlacklistBuilder(rs.getString("id"))
                .setName(rs.getString("name"))
                .setOwnerId(rs.getString("owner_id"))
                .setAdded(new Timestamp(rs.getLong("added")))
                .build();
    }
}
