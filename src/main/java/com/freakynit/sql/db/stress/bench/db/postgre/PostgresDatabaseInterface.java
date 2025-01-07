package com.freakynit.sql.db.stress.bench.db.postgre;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.db.AbstractDatabaseInterface;
import com.freakynit.sql.db.stress.bench.db.DatabaseInterface;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.util.Map;

public class PostgresDatabaseInterface extends AbstractDatabaseInterface implements DatabaseInterface<Connection> {
    private final PostgreConfig postgreConfig;

    public PostgresDatabaseInterface(Map<String, Object> dbConfig) {
        this.postgreConfig = new ObjectMapper().convertValue(dbConfig, PostgreConfig.class);
    }

    @Override
    public Connection getConnection() throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgreConfig.getConnectionUrl());
        dataSource.setUser(postgreConfig.getUsername());
        dataSource.setPassword(postgreConfig.getPassword());

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
