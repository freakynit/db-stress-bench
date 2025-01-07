package com.freakynit.sql.db.stress.bench.configs;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DBConfig {
    private String impl;
    private Map<String, Object> properties;
    private List<QueryPayload> queryPayloads;
}
