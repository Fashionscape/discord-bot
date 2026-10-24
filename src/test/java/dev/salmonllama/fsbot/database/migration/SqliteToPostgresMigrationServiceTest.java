package dev.salmonllama.fsbot.database.migration;

import dev.salmonllama.fsbot.database.entities.*;
import dev.salmonllama.fsbot.database.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SqliteToPostgresMigrationServiceTest {

    @Autowired
    private SqliteToPostgresMigrationService migrationService;

    @Autowired
    private OutfitRepository outfitRepository;

    @Autowired
    private GalleryChannelRepository galleryChannelRepository;

    @Autowired
    private ServerConfigRepository serverConfigRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private BlockedServerRepository blockedServerRepository;

    @Autowired
    private StaticPermissionRepository staticPermissionRepository;

    @Autowired
    private ColorRoleRepository colorRoleRepository;

    private Path tempSqliteDb;

    @BeforeEach
    void setUp() throws IOException, SQLException {
        outfitRepository.deleteAll();
        galleryChannelRepository.deleteAll();
        serverConfigRepository.deleteAll();
        blockedUserRepository.deleteAll();
        blockedServerRepository.deleteAll();
        staticPermissionRepository.deleteAll();
        colorRoleRepository.deleteAll();

        tempSqliteDb = Files.createTempFile("fsbot-legacy-test", ".db");
        String url = "jdbc:sqlite:" + tempSqliteDb.toAbsolutePath();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Create legacy SQLite schema
            stmt.execute("CREATE TABLE outfits (id TEXT, link TEXT, submitter TEXT, tag TEXT, meta TEXT, discord_name TEXT, created TEXT, updated TEXT, deleted TEXT, featured TEXT, display_count INT, delete_hash TEXT)");
            stmt.execute("CREATE TABLE galleries (server_id TEXT, channel_id TEXT, emoji TEXT, tag TEXT)");
            stmt.execute("CREATE TABLE server_config (id TEXT, prefix TEXT, welcome_message TEXT, welcome_channel TEXT)");
            stmt.execute("CREATE TABLE blacklist_users (id TEXT, reason TEXT, added TEXT)");
            stmt.execute("CREATE TABLE blacklist_servers (id TEXT, name TEXT, owner_id TEXT, added TEXT)");
            stmt.execute("CREATE TABLE static_permissions (user_id TEXT, permission TEXT, date_added TEXT)");
            stmt.execute("CREATE TABLE color_roles (role_id TEXT, server_id TEXT, color TEXT, group_id TEXT)");

            // Insert sample data
            stmt.execute("INSERT INTO outfits VALUES ('legacy-1', 'https://i.imgur.com/sample1.png', 'sub-1', 'osrs', 'meta-1', 'Player1', '2022-01-01 12:00:00', '2022-01-02 12:00:00', 'false', 'true', 10, 'delhash1')");
            stmt.execute("INSERT INTO galleries VALUES ('srv-1', 'chan-1', ':heart:', 'fashion')");
            stmt.execute("INSERT INTO server_config VALUES ('srv-1', '!', 'Welcome to our server', 'chan-welcome')");
            stmt.execute("INSERT INTO blacklist_users VALUES ('bad-user-1', 'Rule breaker', '2023-05-01 00:00:00')");
            stmt.execute("INSERT INTO blacklist_servers VALUES ('bad-srv-1', 'Raiders', 'owner-1', '2023-06-01 00:00:00')");
            stmt.execute("INSERT INTO static_permissions VALUES ('admin-user', 'ADMIN', '2021-08-01 00:00:00')");
            stmt.execute("INSERT INTO color_roles VALUES ('role-red', 'srv-1', '#FF0000', 'group-1')");
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempSqliteDb != null && Files.exists(tempSqliteDb)) {
            Files.deleteIfExists(tempSqliteDb);
        }
    }

    @Test
    void testFullMigration() throws SQLException {
        SqliteToPostgresMigrationService.MigrationReport report = migrationService.migrate(tempSqliteDb);

        assertEquals(0, report.outfitsMigrated);
        assertEquals(1, report.outfitsSkipped);
        assertEquals(1, report.galleriesMigrated);
        assertEquals(1, report.serverConfigsMigrated);
        assertEquals(1, report.blockedUsersMigrated);
        assertEquals(1, report.blockedServersMigrated);
        assertEquals(1, report.staticPermissionsMigrated);
        assertEquals(1, report.colorRolesMigrated);
        assertTrue(report.errors.isEmpty(), "Expected zero migration errors");

        // Legacy outfits have no server or channel ID, so they are skipped
        assertTrue(outfitRepository.findById("legacy-1").isEmpty());

        Optional<GalleryChannelEntity> gallery = galleryChannelRepository.findById("chan-1");
        assertTrue(gallery.isPresent());
        assertEquals("srv-1", gallery.get().getServerId());

        Optional<ServerConfigEntity> serverConfig = serverConfigRepository.findById("srv-1");
        assertTrue(serverConfig.isPresent());
        assertEquals("!", serverConfig.get().getPrefix());

        assertTrue(blockedUserRepository.existsById("bad-user-1"));
        assertTrue(blockedServerRepository.existsById("bad-srv-1"));
        assertTrue(staticPermissionRepository.existsByUserIdAndPermission("admin-user", "ADMIN"));

        Optional<ColorRoleEntity> role = colorRoleRepository.findByRoleId("role-red");
        assertTrue(role.isPresent());
        assertEquals("#FF0000", role.get().getColor());
    }
}
