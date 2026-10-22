package dev.salmonllama.fsbot.database.controllers;

import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.database.models.UserBlacklist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class UserBlacklistController {
    // Need to create, read, and delete. No need for update?
    // Exists, insert, get, delete
    public static CompletableFuture<Void> insert(UserBlacklist bl) {
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

    public static CompletableFuture<Boolean> exists(UserBlacklist bl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return existsExec(bl.getId());
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<UserBlacklist>> get(String id) {
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

    public static CompletableFuture<Void> delete(UserBlacklist bl) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteExec(bl.getId());
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static void insertExec(UserBlacklist bl) throws SQLException {
        if (bl.getAdded() == null) {
            bl.setAdded(new Timestamp(System.currentTimeMillis()));
        }

        FSDB.get().insert("INSERT INTO blacklist_users ('id', 'reason', 'added') VALUES (?, ?, ?)",
                bl.getId(),
                bl.getReason(),
                bl.getAdded()
        );
    }

    private static boolean existsExec(String id) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT EXISTS(SELECT 1 FROM blacklist_users WHERE id = ?) AS hmm", id);
        boolean exists = rs.getBoolean("hmm");

        FSDB.get().close(rs);
        return exists;
    }

    private static Optional<UserBlacklist> getExec(String id) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM blacklist_users WHERE id = ?", id);

        if (rs.next()) {
            UserBlacklist bl = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(bl);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static void deleteExec(String id) throws SQLException {
        FSDB.get().query("DELETE FROM blacklist_users WHERE id = ?", id);
    }

    private static UserBlacklist mapObject(ResultSet rs) throws SQLException {
        return new UserBlacklist.Builder(rs.getString("id"))
                .setReason(rs.getString("reason"))
                .setAdded(new Timestamp(rs.getLong("added")))
                .build();
    }
}
