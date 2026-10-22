package dev.salmonllama.fsbot.database.controllers;

import dev.salmonllama.fsbot.database.FSDB;
import dev.salmonllama.fsbot.database.models.Outfit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class OutfitController {
    // There is no delete method. 'Deletions' should be executed as an update to the deleted flag.
    public static CompletableFuture<Void> insert(Outfit outfit) {
        return CompletableFuture.runAsync(() -> {
            try {
                insertExec(outfit);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Outfit>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findByIdExec(id);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Outfit>> findRandom() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findRandomExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Collection<Outfit>>> findRandom(int amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findRandomExec(amount);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Outfit>> findRandomByTag(String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findRandomByTagExec(tag);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Collection<Outfit>>> findRandomByTag(String tag, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findRandomByTagExec(tag, amount);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Outfit>> findRandomBySubmitter(String submitterId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findRandomBySubmitterExec(submitterId);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Optional<Collection<Outfit>>> findRandomBySubmitter(String submitterId, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findRandomBySubmitterExec(submitterId, amount);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Integer> countTotalOutfits() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countTotalOutfitsExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Integer> countDeletedOutfits() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countDeletedOutfitsExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Integer> countActiveOutfits() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countActiveOutfitsExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Integer> countFeaturedOutfits() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countFeaturedOutfitsExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Integer> countOutfits(boolean deleted, boolean featured) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countOutfitsExec(deleted, featured);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Integer> countOutfitsBySubmitter(String submitterId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countOutfitsBySubmitterExec(submitterId);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<List<String>> getDistinctTags() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getDistinctTagsExec();
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> update(Outfit outfit) {
        return CompletableFuture.runAsync(() -> {
            try {
                updateExec(outfit);
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

    public static CompletableFuture<Void> delete(Outfit outfit) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteExec(outfit.getId());
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> restore(Outfit outfit) {
        return CompletableFuture.runAsync(() -> {
            try {
                Outfit newOutfit = new Outfit.OutfitBuilder(outfit).setDeleted(false).build();
                System.out.println(newOutfit.isDeleted());
                updateExec(newOutfit);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static CompletableFuture<Void> forceRemove(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                forceRemoveExec(id);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static void insertExec(Outfit outfit) throws SQLException {
        if (outfit.getCreated() == null) {
            outfit.setCreated(new Timestamp(System.currentTimeMillis()));
        }

        if (outfit.getUpdated() == null) {
            outfit.setUpdated(new Timestamp(System.currentTimeMillis()));
        }

        FSDB.get().insert(
                    "INSERT INTO " +
                        "outfits('id', 'link', 'submitter', 'tag', 'meta', 'discord_name', 'created', 'updated', 'deleted', 'featured', 'display_count', 'delete_hash') " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                outfit.getId(),
                outfit.getLink(),
                outfit.getSubmitter(),
                outfit.getTag(),
                outfit.getMeta(),
                outfit.getDiscordName(),
                outfit.getCreated(),
                outfit.getUpdated(),
                outfit.isDeleted(),
                outfit.isFeatured(),
                outfit.getDisplayCount(),
                outfit.getDeleteHash()
        );
    }

    private static Optional<Outfit> findByIdExec(String id) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM outfits WHERE id = ?", id);

        if (rs.next()) {
            Outfit outfit = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(outfit);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static Optional<Outfit> findRandomExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM outfits WHERE deleted = 0 ORDER BY random() LIMIT 1");

        if (rs.next()) {
            Outfit outfit = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(outfit);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static Optional<Collection<Outfit>> findRandomExec(int amount) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM outfits WHERE deleted = 0 ORDER BY random() LIMIT ?", amount);

        return extractMultiple(rs);
    }

    private static Optional<Outfit> findRandomByTagExec(String tag) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM outfits WHERE tag = ? AND deleted = 0 ORDER BY random() LIMIT 1", tag);

        if (rs.next()) {
            Outfit outfit = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(outfit);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static Optional<Collection<Outfit>> findRandomByTagExec(String tag, int amount) throws SQLException { // Still has to be an optional, the tag may not exist. It is user-supplied.
        ResultSet rs = FSDB.get().select("SELECT  * FROM outfits WHERE tag = ? AND deleted = 0 ORDER BY  random() LIMIT ?", tag, amount);

        return extractMultiple(rs);
    }

    private static Optional<Outfit> findRandomBySubmitterExec(String submitterId) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM outfits WHERE submitter = ? AND deleted = 0 ORDER BY random() LIMIT 1", submitterId);

        if (rs.next()) {
            Outfit outfit = mapObject(rs);
            FSDB.get().close(rs);
            return Optional.of(outfit);
        }

        FSDB.get().close(rs);
        return Optional.empty();
    }

    private static Optional<Collection<Outfit>> findRandomBySubmitterExec(String submitterId, int amount) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT * FROM outfits WHERE submitter = ? AND deleted = 0 ORDER BY random() LIMIT ?", submitterId, amount);

        return extractMultiple(rs);
    }

    private static int countTotalOutfitsExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT COUNT(*) AS count FROM outfits");

        return extractCount(rs);
    }

    private static int countDeletedOutfitsExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT COUNT(*) AS count FROM outfits WHERE deleted = 1");

        return extractCount(rs);
    }

    private static int countActiveOutfitsExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT COUNT(*) AS count FROM outfits WHERE deleted = 0");

        return extractCount(rs);
    }

    private static int countFeaturedOutfitsExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT COUNT(*) AS count FROM outfits WHERE featured = 1");

        return extractCount(rs);
    }

    private static int countOutfitsExec(boolean deleted, boolean featured) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT COUNT(*) AS count FROM outfits WHERE deleted = ? AND featured = ?", deleted, featured);

        return extractCount(rs);
    }

    private static int countOutfitsBySubmitterExec(String submitterId) throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT COUNT(*) AS count FROM outfits WHERE submitter = ? AND deleted = 0", submitterId);

        return extractCount(rs);
    }

    private static List<String> getDistinctTagsExec() throws SQLException {
        ResultSet rs = FSDB.get().select("SELECT DISTINCT tag FROM outfits");

        List<String> tags = new ArrayList<>();
        while (rs.next()) {
            tags.add(rs.getString("tag"));
        }

        FSDB.get().close(rs);
        return tags;
    }

    private static void updateExec(Outfit outfit) throws SQLException {
        FSDB.get().query("UPDATE outfits SET " +
                "link = ?," +
                "submitter = ?," +
                "tag = ?," +
                "meta = ?," +
                "discord_name = ?," +
                "updated = ?," +
                "featured = ?," +
                "deleted = ?," +
                "display_count = ?" +
                "WHERE id = ?",
                outfit.getLink(),
                outfit.getSubmitter(),
                outfit.getTag(),
                outfit.getMeta(),
                outfit.getDiscordName(),
                outfit.getUpdated(),
                outfit.isFeatured(),
                outfit.isDeleted(),
                outfit.getDisplayCount(),
                outfit.getId());
    }

    private static void deleteExec(String id) throws SQLException {
        FSDB.get().query("UPDATE outfits SET deleted = true WHERE id = ?", id);
    }

    private static void forceRemoveExec(String id) throws SQLException {
        FSDB.get().query("DELETE FROM outfits WHERE id = ?", id);
    }

    private static Optional<Collection<Outfit>> extractMultiple(ResultSet rs) throws SQLException {
        Collection<Outfit> outfits = new ArrayList<>();
        
        while (rs.next()) {
            outfits.add(mapObject(rs));
        }

        FSDB.get().close(rs);

        if (outfits.size() == 0) {
            return Optional.empty();
        }
        
        return Optional.of(outfits);
    }

    private static int extractCount(ResultSet rs) throws SQLException {
        int count = 0;
        if (rs.next()) {
            count = rs.getInt("count");
        }

        FSDB.get().close(rs);
        return count;
    }

    private static Outfit mapObject(ResultSet rs) throws SQLException {
        return new Outfit.OutfitBuilder()
                .setId(rs.getString("id"))
                .setLink(rs.getString("link"))
                .setSubmitter(rs.getString("submitter"))
                .setTag(rs.getString("tag"))
                .setMeta(rs.getString("meta"))
                .setDiscordName(rs.getString("discord_name"))
                .setCreated(new Timestamp(rs.getLong("created")))
                .setUpdated(new Timestamp(rs.getLong("updated")))
                .setDeleted(rs.getBoolean("deleted"))
                .setFeatured(rs.getBoolean("featured"))
                .setDisplayCount(rs.getInt("display_count"))
                .setDeleteHash(rs.getString("delete_hash"))
                .build();
    }
}
