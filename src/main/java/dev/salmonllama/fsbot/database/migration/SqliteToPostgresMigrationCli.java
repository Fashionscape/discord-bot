package dev.salmonllama.fsbot.database.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SqliteToPostgresMigrationCli implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SqliteToPostgresMigrationCli.class);
    private static final String MIGRATE_FLAG = "--migrate-sqlite";

    private final SqliteToPostgresMigrationService migrationService;

    public SqliteToPostgresMigrationCli(SqliteToPostgresMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(String... args) throws Exception {
        for (String arg : args) {
            if (arg.startsWith(MIGRATE_FLAG)) {
                String pathStr;
                if (arg.contains("=")) {
                    pathStr = arg.substring(arg.indexOf("=") + 1).trim();
                } else {
                    pathStr = "fsbot.db";
                }

                Path path = Paths.get(pathStr);
                logger.info("Executing SQLite to PostgreSQL migration for: {}", path.toAbsolutePath());

                SqliteToPostgresMigrationService.MigrationReport report = migrationService.migrate(path);
                logger.info("Migration complete. Summary: {}", report);
            }
        }
    }
}
