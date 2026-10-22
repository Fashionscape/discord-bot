// Heavily inspired by Kaaz's Emily database connection: https://github.com/Kaaz/DiscordBot/tree/master/src/main/java/emily/db
package dev.salmonllama.fsbot.database;

import dev.salmonllama.fsbot.exceptions.UnknownParameterException;
import dev.salmonllama.fsbot.utilities.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class DatabaseProvider {
    private final String DB_ADDR;
    private final String DB_NAME;
    private Connection c;

    private final static Logger logger = LoggerFactory.getLogger(DatabaseProvider.class);

    public DatabaseProvider(String dbName) {
        DB_NAME = dbName;
        try {
            Files.createDirectories(Constants.BOT_FOLDER);
        } catch (IOException e) {
            logger.error("Could not create bot config directory: {}", Constants.BOT_FOLDER, e);
        }
        Path addr = Constants.BOT_FOLDER.resolve(Constants.DB_NAME + ".db");
        DB_ADDR = "jdbc:sqlite:".concat(addr.toString());

        logger.info("Initializing database...");
        logger.info(DB_ADDR);
    }

    private Connection createConnection() {
        try {
            SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
            dataSource.setDatabaseName(DB_NAME);
            dataSource.setUrl(DB_ADDR);
            c = dataSource.getConnection();
            return c;
        } catch (SQLException e) {
            logger.error("Could not connect to database at {}: {}", DB_ADDR, e.getMessage());
            throw new RuntimeException("Could not connect to database: " + DB_ADDR, e);
        }
    }

    public Connection getConnection() {
        if (c ==  null) {
            return createConnection();
        }
        return c;
    }

    private void resolveParameters(PreparedStatement query, Object... params) throws SQLException {
        int index = 1;
        for (Object p : params) {
            if (p instanceof String) {
                query.setString(index, (String) p);
            }
            else if (p instanceof Integer) {
                query.setInt(index, (int) p);
            }
            else if (p instanceof Boolean) {
                query.setBoolean(index, (boolean) p);
            }
            else if (p instanceof Timestamp) {
                query.setTimestamp(index, (Timestamp) p);
            } else if (p == null) {
                query.setNull(index, Types.NULL);
            } else {
                throw new UnknownParameterException(p, index);
            }
            index++;
        }
    }

    public int query(String sql) throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    public int query(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            resolveParameters(stmt, params);
            return stmt.executeUpdate();
        }
    }

    public int insert(String sql, Object... params) throws SQLException {
        try (
                PreparedStatement query = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ResultSet rs = query.getGeneratedKeys()
        ) {
            resolveParameters(query, params);
            query.executeUpdate();

            if (rs.next()) {
                return rs.getInt(1);
            }

            return -1;
        }
    }

    public ResultSet select(String sql, Object... params) throws SQLException {
        PreparedStatement query;
        query = getConnection().prepareStatement(sql);
        resolveParameters(query, params);
        return query.executeQuery();
    }

    public void close(ResultSet rs) throws SQLException {
        rs.getStatement().close();
        rs.close();
    }
}
