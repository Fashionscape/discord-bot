package dev.salmonllama.fsbot.database.controllers;

import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.database.models.StaticPermission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class StaticPermissionController {
    // Need to insert, get all per user, get per keyword, and delete
    public static CompletableFuture<Void> insert(StaticPermission perm) {
        return CompletableFuture.runAsync(() -> {
            try {
                insertExec(perm);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Collection<StaticPermission>>> getByUser(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getByUserExec(userId);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Collection<StaticPermission>>> getByPermission(String perm) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getByPermissionExec(perm);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Collection<StaticPermission>>> getAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getAllExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> delete(StaticPermission perm) {
        return delete(perm.getUserId(), perm.getPermission());
    }

    public static CompletableFuture<Void> delete(String userId, String perm) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteExec(userId, perm);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static void insertExec(StaticPermission perm) throws SQLException {
        if (perm.getAdded() == null) {
            perm.setAdded(new Timestamp(System.currentTimeMillis()));
        }

        FSDB.get().insert("INSERT INTO static_permissions (user_id, permission, date_added) VALUES (?, ?, ?)",
                perm.getUserId(),
                perm.getPermission(),
                perm.getAdded()
        );
    }

    private static Optional<Collection<StaticPermission>> getByUserExec(String userId) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM static_permissions WHERE user_id = ?", userId);

        Collection<StaticPermission> permissions = new ArrayList<>();
        while (rs.next()) {
            permissions.add(mapObject(rs));
        }

        FSDB.get().close(rs);

        if (permissions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(permissions);
    }

    private static Optional<Collection<StaticPermission>> getByPermissionExec(String perm) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM static_permissions WHERE permission = ?", perm);

        Collection<StaticPermission> users = new ArrayList<>();
        while (rs.next()) {
            users.add(mapObject(rs));
        }

        FSDB.get().close(rs);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(users);
    }

    private static Optional<Collection<StaticPermission>> getAllExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM static_permissions");

        Collection<StaticPermission> permissions = new ArrayList<>();
        while (rs.next()) {
            permissions.add(mapObject(rs));
        }

        FSDB.get().close(rs);

        if (permissions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(permissions);
    }

    private static void deleteExec(String userId, String perm) throws SQLException {
        FSDB.get().query("DELETE FROM static_permissions WHERE user_id = ? AND permission = ?", userId, perm);
    }

    private static StaticPermission mapObject(ResultSet rs) throws SQLException {
        return new StaticPermission.StaticPermissionBuilder(rs.getString("user_id"))
                .setPermission(rs.getString("permission"))
                .setAdded(new Timestamp(rs.getLong("date_added")))
                .build();
    }
}
