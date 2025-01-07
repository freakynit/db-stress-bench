package com.freakynit.sql.db.stress.bench.configs;

import lombok.Data;

import java.util.List;

@Data
public class Config {
    private BenchmarkingSpec benchmarkingSpec;
    private ServerConfig serverConfig;
    private List<DBConfig> dbConfigs;
}
