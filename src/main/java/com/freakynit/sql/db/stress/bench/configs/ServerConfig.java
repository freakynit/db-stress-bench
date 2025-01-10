package com.freakynit.sql.db.stress.bench.configs;

import lombok.Data;

@Data
public class ServerConfig {
    private Boolean enableStatsEndpoint;
    private Boolean enablePrometheusMetricsEndpoint;
    private Integer port;
    private Float graphUpdateIntervalSeconds;
}
