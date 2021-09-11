/*
 Copyright (c) 2021 Aleksei Gryczewski

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

// Heavily inspired by Kaaz's Emily database connection: https://github.com/Kaaz/DiscordBot/tree/master/src/main/java/emily/db
package dev.salmonllama.fsbot.database;

import dev.salmonllama.fsbot.exceptions.UnknownParameterException;
import dev.salmonllama.fsbot.config.BotConfig;
import dev.salmonllama.fsbot.utilities.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.nio.file.Path;
import java.sql.*;

public class DatabaseProvider {
    private final String DB_ADDR;
    private final String DB_NAME;
    private Connection c;

    private final static Logger logger = LoggerFactory.getLogger(DatabaseProvider.class);

    public DatabaseProvider(String dbName) {
        DB_NAME = dbName;
        Path addr = Path.of(Constants.BOT_FOLDER.toString(), BotConfig.DB_ADDR);
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
            e.printStackTrace();
            System.out.println("Could not connect to database, double check config values");
            System.exit(1);
        }
        return null;
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
