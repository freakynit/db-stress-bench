package com.freakynit.sql.db.stress.bench.configs;

import lombok.Data;

import java.util.List;

@Data
public class QueryPayload {
    private String filePath;
    private String name;
    private List<String> columns;
    private String query;
    private Integer timeoutSeconds;
    private Boolean consumeResultSet;
}
