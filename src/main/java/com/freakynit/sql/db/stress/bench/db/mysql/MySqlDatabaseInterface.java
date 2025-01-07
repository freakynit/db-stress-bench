package com.freakynit.sql.db.stress.bench.db.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.db.AbstractDatabaseInterface;
import com.freakynit.sql.db.stress.bench.db.DatabaseInterface;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.*;
import java.util.Map;

public class MySqlDatabaseInterface extends AbstractDatabaseInterface implements DatabaseInterface<Connection> {
    private final MysqlConfig mysqlConfig;

    public MySqlDatabaseInterface(Map<String, Object> dbConfig) {
        this.mysqlConfig = new ObjectMapper().convertValue(dbConfig, MysqlConfig.class);
    }

    @Override
    public Connection getConnection() throws Exception {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(mysqlConfig.getConnectionUrl());
        dataSource.setUser(mysqlConfig.getUsername());
        dataSource.setPassword(mysqlConfig.getPassword());
        dataSource.setAutoReconnect(mysqlConfig.getAutoReconnect());

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
