package dev.salmonllama.fsbot.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class OutfitUuidMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("fashionscape_test")
            .withUsername("fsbot_test")
            .withPassword("fsbot_test");

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    @Test
    @DisplayName("V6 gives 8-character outfit IDs a UUID and carries it to votes and audit entries")
    void testShortIdsAreRemapped() {
        flyway("5").migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));

        String uuidId = "3f2b8c1e-9d4a-4e7b-8c2f-1a6d5e9b0c47";
        for (String id : new String[]{"abcd1234", uuidId}) {
            jdbc.update("INSERT INTO outfits (id, link, submitter, server_id, channel_id) VALUES (?, ?, 'user-1', 'server-1', 'channel-1')",
                    id, "https://media.fashionscape.org/outfits/" + id + ".png");
        }
        jdbc.update("INSERT INTO outfit_votes (outfit_id, user_id) VALUES ('abcd1234', 'voter-1')");
        jdbc.update("INSERT INTO audit_log (action, actor_id, target_type, target_id) VALUES ('OUTFIT_ADDED', 'user-1', 'outfit', 'abcd1234')");

        flyway("latest").migrate();

        UUID remapped = jdbc.queryForObject(
                "SELECT id FROM outfits WHERE link = 'https://media.fashionscape.org/outfits/abcd1234.png'", UUID.class);
        assertNotNull(remapped);
        assertEquals(UUID.fromString(uuidId), jdbc.queryForObject(
                "SELECT id FROM outfits WHERE link LIKE '%" + uuidId + "%'", UUID.class));
        assertEquals(remapped, jdbc.queryForObject("SELECT outfit_id FROM outfit_votes WHERE user_id = 'voter-1'", UUID.class));
        assertEquals(remapped.toString(), jdbc.queryForObject("SELECT target_id FROM audit_log", String.class));

        // Raw SQL inserts get an ID from the column default
        jdbc.update("INSERT INTO outfits (link, submitter, server_id, channel_id) VALUES ('https://x/new.png', 'user-1', 'server-1', 'channel-1')");
        assertNotNull(jdbc.queryForObject("SELECT id FROM outfits WHERE link = 'https://x/new.png'", UUID.class));
    }
}
