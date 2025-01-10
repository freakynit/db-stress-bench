package com.freakynit.sql.db.stress.bench.db.cassandradb;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.db.AbstractDatabaseInterface;
import com.freakynit.sql.db.stress.bench.db.DatabaseInterface;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class CassandraDBDatabaseInterface extends AbstractDatabaseInterface implements DatabaseInterface<CqlSession> {
    private final CassandraDBConfig cassandraDBConfig;

    public CassandraDBDatabaseInterface(Map<String, Object> dbConfig) {
        this.cassandraDBConfig = new ObjectMapper().convertValue(dbConfig, CassandraDBConfig.class);
    }

    @Override
    public CqlSession getConnection() throws Exception {
        List<EndPoint> contactPoints = Arrays.stream(this.cassandraDBConfig.getContactPoints().split(","))
                .map(cp -> cp.trim().split(":"))
                .map(parts -> new DefaultEndPoint(new InetSocketAddress(parts[0], Integer.parseInt(parts[1]))))
                .collect(Collectors.toList());

        return CqlSession.builder()
                .addContactEndPoints(contactPoints)
                .withLocalDatacenter(this.cassandraDBConfig.getDatacenter())
                .withKeyspace(this.cassandraDBConfig.getKeyspace())
                .build();
    }

    @Override
    public int executeQuery(CqlSession connectionObject, String query, boolean consumeResultSet, Integer timeoutSeconds) throws Exception {
        SimpleStatement statement = SimpleStatement.newInstance(query);
        statement.setTimeout(Duration.ofSeconds(timeoutSeconds));

        int count = 0;

        ResultSet rs = connectionObject.execute(statement);
        if(consumeResultSet) {
            Iterator<Row> it = rs.iterator();
            while (it.hasNext()) {
                it.next();
                count++;
            }
        }

        return count;
    }

    @Override
    public void closeConnectionObject(CqlSession connectionObject) {
        connectionObject.close();
    }
}

