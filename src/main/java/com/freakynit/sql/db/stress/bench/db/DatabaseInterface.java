package com.freakynit.sql.db.stress.bench.db;

public interface DatabaseInterface<T> {
    T getConnection() throws Exception;
    int executeQuery(T connectionObject, String query, boolean consumeResultSet, Integer timeoutSeconds) throws Exception;
    void closeConnectionObject(T connectionObject);
}
