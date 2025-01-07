package com.freakynit.sql.db.stress.bench.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractDatabaseInterface {
    public static final Logger logger = LoggerFactory.getLogger(AbstractDatabaseInterface.class);

    public int executeQuery(Connection connectionObject, String query, boolean consumeResultSet, Integer timeoutSeconds) throws Exception {
        int count = 0;
        Statement statement = null;
        ResultSet rs = null;

        try {
            statement = connectionObject.createStatement();
            statement.setQueryTimeout(timeoutSeconds);

            rs = statement.executeQuery(query);

            if(consumeResultSet) {
                while (rs.next()) {
                    count++;
                }
            }

            statement.close();
            rs.close();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignored) {}
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {}
            }
        }

        return count;
    }

    public void closeConnectionObject(Connection connectionObject) {
        try {
            connectionObject.close();
        } catch (SQLException e) {
            logger.error("Exception closing connection object", e);
        }
    }
}
