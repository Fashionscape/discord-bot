package dev.salmonllama.fsbot.database.controllers;


public class ColorRoleController {
    // Need insert, get by color, exists by color, exists by role, get by server, count, and delete
/*    public static CompletableFuture<Void> insert(ColorRole cr) {
        return CompletableFuture.runAsync(() -> {
            try {
                insertExec(cr);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Boolean> exists(String color) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return existsExec(color);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Boolean> exists(long roleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return existsExec(roleId);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<ColorRole>> get(String color) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getExec(color);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<ColorRole>> get(long roleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getExec(roleId);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Collection<ColorRole>>> getAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getAllExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> delete(long roleId) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteExec(roleId);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> delete(String color) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteExec(color);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static void insertExec(ColorRole cr) throws SQLException {
        FSDB.get().insert("INSERT INTO color_roles (role_id, server_id, color) VALUES (?, ?, ?)",
                cr.getRoleId(),
                cr.getServerId(),
                cr.getColor()
        );
    }

    private static boolean existsExec(String color) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT EXISTS(SELECT 1 FROM color_roles WHERE color = ?) AS hmm", color);
        boolean exists = rs.getBoolean("hmm");

        FSDB.get().close(rs);
        return exists;
    }

    private static boolean existsExec(long roleId) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT EXISTS(SELECT 1 FROM color_roles WHERE role_id = ?) AS hmm", roleId);
        boolean exists = rs.getBoolean("hmm");

        FSDB.get().close(rs);
        return exists;
    }

    private static Optional<ColorRole> getExec(String color) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM color_roles WHERE color = ?", color);

        if (rs.next()) {
            ColorRole colorRole = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(colorRole);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static Optional<ColorRole> getExec(Long roleId) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM color_roles WHERE role_id = ?", roleId);

        if (rs.next()) {
            ColorRole colorRole = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(colorRole);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static Optional<Collection<ColorRole>> getAllExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM color_roles");

        Collection<ColorRole> roles = new ArrayList<>();
        while (rs.next()) {
            roles.add(mapObject(rs));
            FSDB.get().close(rs);
        }

        if (roles.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(roles);
    }

    private static void deleteExec(long roleId) throws SQLException {
        FSDB.get().query("DELETE FROM color_roles WHERE role_id = ?", roleId);
    }

    private static void deleteExec(String color) throws SQLException {
        FSDB.get().query("DELETE FROM color_roles WHERE color = ?", color);
    }

    private static ColorRole mapObject(ResultSet rs) throws SQLException {
        return new ColorRole.ColorRoleBuilder(rs.getLong("role_id"))
                .setServerId(rs.getLong("server_id"))
                .setColor(rs.getString("color"))
                .build();
    }*/
}