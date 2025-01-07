package com.freakynit.sql.db.stress.bench.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class StatsApiResponseModel {
    private Boolean isRunning;
    private long timestamp;
    private Map<String, Map<String, Number>> metrics;
}
