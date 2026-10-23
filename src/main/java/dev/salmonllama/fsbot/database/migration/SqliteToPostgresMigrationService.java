package dev.salmonllama.fsbot.database.migration;

import dev.salmonllama.fsbot.database.entities.*;
import dev.salmonllama.fsbot.database.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SqliteToPostgresMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(SqliteToPostgresMigrationService.class);

    private final GalleryChannelRepository galleryChannelRepository;
    private final ServerConfigRepository serverConfigRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final BlockedServerRepository blockedServerRepository;
    private final StaticPermissionRepository staticPermissionRepository;
    private final ColorRoleRepository colorRoleRepository;

    public SqliteToPostgresMigrationService(
            GalleryChannelRepository galleryChannelRepository,
            ServerConfigRepository serverConfigRepository,
            BlockedUserRepository blockedUserRepository,
            BlockedServerRepository blockedServerRepository,
            StaticPermissionRepository staticPermissionRepository,
            ColorRoleRepository colorRoleRepository
    ) {
        this.galleryChannelRepository = galleryChannelRepository;
        this.serverConfigRepository = serverConfigRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.blockedServerRepository = blockedServerRepository;
        this.staticPermissionRepository = staticPermissionRepository;
        this.colorRoleRepository = colorRoleRepository;
    }

    @Transactional
    public MigrationReport migrate(Path sqliteDbPath) throws SQLException {
        if (!Files.exists(sqliteDbPath)) {
            throw new IllegalArgumentException("SQLite database file not found at: " + sqliteDbPath.toAbsolutePath());
        }

        logger.info("Starting SQLite-to-PostgreSQL migration from: {}", sqliteDbPath.toAbsolutePath());
        String url = "jdbc:sqlite:" + sqliteDbPath.toAbsolutePath();

        MigrationReport report = new MigrationReport();

        try (Connection conn = DriverManager.getConnection(url)) {
            migrateOutfits(conn, report);
            migrateGalleries(conn, report);
            migrateServerConfigs(conn, report);
            migrateBlockedUsers(conn, report);
            migrateBlockedServers(conn, report);
            migrateStaticPermissions(conn, report);
            migrateColorRoles(conn, report);
        }

        logger.info("SQLite-to-PostgreSQL migration completed successfully: {}", report);
        return report;
    }

    /**
     * Legacy outfits don't record the server and channel they were submitted in, which v3
     * requires on every outfit. They are counted and skipped until a dedicated import can
     * supply that origin.
     */
    private void migrateOutfits(Connection conn, MigrationReport report) {
        if (!tableExists(conn, "outfits")) {
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM outfits");
             ResultSet rs = stmt.executeQuery()) {

            report.outfitsSkipped = rs.next() ? rs.getInt(1) : 0;
            logger.warn("Skipped {} legacy outfits: they have no server or channel ID, which v3 requires", report.outfitsSkipped);
        } catch (SQLException e) {
            logger.error("Error counting legacy outfits: {}", e.getMessage(), e);
            report.errors.add("outfits: " + e.getMessage());
        }
    }

    private void migrateGalleries(Connection conn, MigrationReport report) {
        if (!tableExists(conn, "galleries")) {
            return;
        }

        String sql = "SELECT server_id, channel_id, emoji, tag FROM galleries";
        List<GalleryChannelEntity> entities = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String channelId = rs.getString("channel_id");
                String serverId = rs.getString("server_id");

                if (channelId == null || serverId == null) {
                    continue;
                }

                GalleryChannelEntity entity = new GalleryChannelEntity(
                        channelId,
                        serverId,
                        rs.getString("emoji"),
                        rs.getString("tag"),
                        true
                );
                entities.add(entity);
            }

            galleryChannelRepository.saveAll(entities);
            report.galleriesMigrated = entities.size();
            logger.info("Migrated {} gallery channels", entities.size());
        } catch (SQLException e) {
            logger.error("Error migrating galleries: {}", e.getMessage(), e);
            report.errors.add("galleries: " + e.getMessage());
        }
    }

    private void migrateServerConfigs(Connection conn, MigrationReport report) {
        if (!tableExists(conn, "server_config")) {
            return;
        }

        String sql = "SELECT id, prefix, welcome_message, welcome_channel FROM server_config";
        List<ServerConfigEntity> entities = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                if (id == null) {
                    continue;
                }

                ServerConfigEntity entity = new ServerConfigEntity(
                        id,
                        rs.getString("prefix"),
                        rs.getString("welcome_message"),
                        rs.getString("welcome_channel")
                );
                entities.add(entity);
            }

            serverConfigRepository.saveAll(entities);
            report.serverConfigsMigrated = entities.size();
            logger.info("Migrated {} server configs", entities.size());
        } catch (SQLException e) {
            logger.error("Error migrating server configs: {}", e.getMessage(), e);
            report.errors.add("server_config: " + e.getMessage());
        }
    }

    private void migrateBlockedUsers(Connection conn, MigrationReport report) {
        if (!tableExists(conn, "blacklist_users")) {
            return;
        }

        String sql = "SELECT id, reason, added FROM blacklist_users";
        List<BlockedUserEntity> entities = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                if (id == null) {
                    continue;
                }

                BlockedUserEntity entity = new BlockedUserEntity(
                        id,
                        rs.getString("reason"),
                        parseInstant(rs.getString("added"))
                );
                entities.add(entity);
            }

            blockedUserRepository.saveAll(entities);
            report.blockedUsersMigrated = entities.size();
            logger.info("Migrated {} blocked users", entities.size());
        } catch (SQLException e) {
            logger.error("Error migrating blocked users: {}", e.getMessage(), e);
            report.errors.add("blacklist_users: " + e.getMessage());
        }
    }

    private void migrateBlockedServers(Connection conn, MigrationReport report) {
        if (!tableExists(conn, "blacklist_servers")) {
            return;
        }

        String sql = "SELECT id, name, owner_id, added FROM blacklist_servers";
        List<BlockedServerEntity> entities = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                if (id == null) {
                    continue;
                }

                BlockedServerEntity entity = new BlockedServerEntity(
                        id,
                        rs.getString("name"),
                        rs.getString("owner_id"),
                        null,
                        parseInstant(rs.getString("added"))
                );
                entities.add(entity);
            }

            blockedServerRepository.saveAll(entities);
            report.blockedServersMigrated = entities.size();
            logger.info("Migrated {} blocked servers", entities.size());
        } catch (SQLException e) {
            logger.error("Error migrating blocked servers: {}", e.getMessage(), e);
            report.errors.add("blacklist_servers: " + e.getMessage());
        }
    }

    private void migrateStaticPermissions(Connection conn, MigrationReport report) {
        if (!tableExists(conn, "static_permissions")) {
            return;
        }

        String sql = "SELECT user_id, permission, date_added FROM static_permissions";
        List<StaticPermissionEntity> entities = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String userId = rs.getString("user_id");
                String permission = rs.getString("permission");
                if (userId == null || permission == null) {
                    continue;
                }

                StaticPermissionEntity entity = new StaticPermissionEntity(
                        userId,
                        permission,
                        parseInstant(rs.getString("date_added"))
                );
                entities.add(entity);
            }

            staticPermissionRepository.saveAll(entities);
            report.staticPermissionsMigrated = entities.size();
            logger.info("Migrated {} static permissions", entities.size());
        } catch (SQLException e) {
            logger.error("Error migrating static permissions: {}", e.getMessage(), e);
            report.errors.add("static_permissions: " + e.getMessage());
        }
    }

    private void migrateColorRoles(Connection conn, MigrationReport report) {
        if (!tableExists(conn, "color_roles")) {
            return;
        }

        String sql = "SELECT role_id, server_id, color, group_id FROM color_roles";
        List<ColorRoleEntity> entities = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String roleId = rs.getString("role_id");
                String serverId = rs.getString("server_id");
                if (roleId == null || serverId == null) {
                    continue;
                }

                ColorRoleEntity entity = new ColorRoleEntity(
                        roleId,
                        serverId,
                        rs.getString("color"),
                        rs.getString("group_id")
                );
                entities.add(entity);
            }

            colorRoleRepository.saveAll(entities);
            report.colorRolesMigrated = entities.size();
            logger.info("Migrated {} color roles", entities.size());
        } catch (SQLException e) {
            logger.error("Error migrating color roles: {}", e.getMessage(), e);
            report.errors.add("color_roles: " + e.getMessage());
        }
    }

    private boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Instant parseInstant(String text) {
        if (text == null || text.isBlank()) {
            return Instant.now();
        }

        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Timestamp.valueOf(text).toInstant();
        } catch (IllegalArgumentException ignored) {
        }

        try {
            long epoch = Long.parseLong(text);
            return Instant.ofEpochMilli(epoch);
        } catch (NumberFormatException ignored) {
        }

        return Instant.now();
    }

    public static class MigrationReport {
        public int outfitsMigrated = 0;
        public int outfitsSkipped = 0;
        public int galleriesMigrated = 0;
        public int serverConfigsMigrated = 0;
        public int blockedUsersMigrated = 0;
        public int blockedServersMigrated = 0;
        public int staticPermissionsMigrated = 0;
        public int colorRolesMigrated = 0;
        public List<String> errors = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("MigrationReport[outfits=%d, outfitsSkipped=%d, galleries=%d, configs=%d, blockedUsers=%d, blockedServers=%d, permissions=%d, colorRoles=%d, errors=%d]",
                    outfitsMigrated, outfitsSkipped, galleriesMigrated, serverConfigsMigrated, blockedUsersMigrated,
                    blockedServersMigrated, staticPermissionsMigrated, colorRolesMigrated, errors.size());
        }
    }
}
