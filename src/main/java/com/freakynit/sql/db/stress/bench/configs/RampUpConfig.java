package com.freakynit.sql.db.stress.bench.configs;

import lombok.Data;

@Data
public class RampUpConfig {
    private Boolean enable;
    private Integer durationInSeconds;
}
