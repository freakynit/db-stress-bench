package com.freakynit.sql.db.stress.bench.db.cassandradb;

import lombok.Data;

@Data
public class CassandraDBConfig {
    private String contactPoints;
    private String datacenter;
    private String keyspace;
}
