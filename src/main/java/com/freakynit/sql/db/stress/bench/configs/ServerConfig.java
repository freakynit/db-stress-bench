package com.freakynit.sql.db.stress.bench.configs;

import lombok.Data;

@Data
public class ServerConfig {
    private Boolean enable;
    private Integer port;
    private Float graphUpdateIntervalSeconds;
}
