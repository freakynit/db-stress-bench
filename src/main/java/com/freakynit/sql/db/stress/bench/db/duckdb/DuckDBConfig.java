package com.freakynit.sql.db.stress.bench.db.duckdb;

import lombok.Data;

@Data
public class DuckDBConfig {
    private String filePath;
    private Boolean readOnly;
}
