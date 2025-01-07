package com.freakynit.sql.db.stress.bench.db.sqlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.db.AbstractDatabaseInterface;
import com.freakynit.sql.db.stress.bench.db.DatabaseInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Map;

public class SqliteDatabaseInterface extends AbstractDatabaseInterface implements DatabaseInterface<Connection> {
    public static final Logger logger = LoggerFactory.getLogger(SqliteDatabaseInterface.class);

    private final SqliteConfig sqliteConfig;

    public SqliteDatabaseInterface(Map<String, Object> dbConfig) {
        this.sqliteConfig = new ObjectMapper().convertValue(dbConfig, SqliteConfig.class);
    }

    @Override
    public Connection getConnection() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + sqliteConfig.getFilePath());

        return dataSource.getConnection();
    }

    @Override
    public int executeQuery(Connection connectionObject, String query, boolean consumeResultSet, Integer timeoutSeconds) throws Exception {
        return super.executeQuery(connectionObject, query, consumeResultSet, timeoutSeconds);
    }

    @Override
    public void closeConnectionObject(Connection connectionObject) {
        super.closeConnectionObject(connectionObject);
    }
}
