package com.freakynit.sql.db.stress.bench.configs;

import lombok.Data;

@Data
public class BenchmarkingSpec {
    private int concurrency;
    private int runTimeSeconds;
    private int defaultQueryTimeoutSeconds;
    private Boolean useVirtualThreads;
    private RampUpConfig rampUpConfig;
    private String resultsOutFile;
    private Integer printRunningStatsEveryNSeconds;
    private String activeDatabase;
}
