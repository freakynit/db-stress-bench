package com.freakynit.sql.db.stress.bench.db.duckdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.db.AbstractDatabaseInterface;
import com.freakynit.sql.db.stress.bench.db.DatabaseInterface;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;

public class DuckDBDatabaseInterface extends AbstractDatabaseInterface implements DatabaseInterface<Connection> {
    private final DuckDBConfig duckDBConfig;

    public DuckDBDatabaseInterface(Map<String, Object> dbConfig) throws ClassNotFoundException {
        Class.forName("org.duckdb.DuckDBDriver");
        this.duckDBConfig = new ObjectMapper().convertValue(dbConfig, DuckDBConfig.class);
    }

    @Override
    public Connection getConnection() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("duckdb.read_only", String.valueOf(duckDBConfig.getReadOnly()));

        return DriverManager.getConnection("jdbc:duckdb:" + duckDBConfig.getFilePath(), properties);
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

