package com.freakynit.sql.db.stress.bench;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.freakynit.sql.db.stress.bench.deserializers.MedianStatJsonSerializer;
import com.freakynit.sql.db.stress.bench.deserializers.PercentileStatsJsonSerializer;
import com.freakynit.sql.db.stress.bench.runner.BenchmarksRunner;
import com.freakynit.sql.db.stress.bench.utils.RunningPercentilesCalculator;
import com.freakynit.sql.db.stress.bench.utils.RunningMedianCalculator;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Getter
public class RunningStats {
    @JsonProperty("completed")
    private long succeededCount = 0;
    @JsonProperty("failed")
    private long failedCount = 0;
    @JsonProperty("invalid_payload")
    private long invalidPayloadCount = 0;
    @JsonProperty("total")
    private long totalCount = 0;
    @JsonProperty("qps")
    private double queriesPerSecond = 0;
    @JsonProperty("min_time_ms")
    private long minTimeNanos = Long.MAX_VALUE;
    @JsonProperty("max_time_ms")
    private long maxTimeNanos = 0;
    @JsonProperty("average_time_ms")
    private double avgTimeNanos = 0.0f;
    private long totalQueryTimeNanos = 1;  // init with 1 instead of 0 to prevent divide by zero error without needing if-else checks every time
    @JsonIgnore
    private double sumOfSquaredDifferences = 0.0f;
    @JsonProperty("stddev_time_ms")
    private double stdDevTimeNanos = 0.0f;
    @JsonSerialize(using = PercentileStatsJsonSerializer.class) @JsonProperty("percentiles_time_ms")
    private RunningPercentilesCalculator timeNanosPercentileCalculator = new RunningPercentilesCalculator(0, 100, 100);
    @JsonSerialize(using = MedianStatJsonSerializer.class) @JsonProperty("median_time_ms")
    private RunningMedianCalculator timeNanosMedianCalculator = new RunningMedianCalculator();

    private ReentrantLock lock = new ReentrantLock();

    public Map<String, Number> toMap() {
        Map<String, Number> metrics = new LinkedHashMap<>();
        if(totalCount == 0) {
            return metrics; // return empty map if stats have no data points aggregated yet
        }

        metrics.put("Queries Per Second (completed only)", queriesPerSecond);
        metrics.put("Average Query Time (ms)", avgTimeNanos /1e6);
        metrics.put("Query StdDev (ms)", stdDevTimeNanos /1e6);
        metrics.put("Min Query Time (ms)", minTimeNanos /1e6);
        metrics.put("Max Query Time (ms)", maxTimeNanos /1e6);
        metrics.put("Median Query Time (ms)", timeNanosMedianCalculator.getMedian()/1e6);
        metrics.put("Successfully Completed Queries", succeededCount);
        metrics.put("Failed Queries", failedCount);
        metrics.put("Invalid Input Payloads", invalidPayloadCount);
        metrics.put("Total Queries (completed + failed + invalid payloads)", totalCount);
        metrics.put("Percentile(20'th) (ms)", timeNanosPercentileCalculator.estimatePercentile(20)/1e6);
        metrics.put("Percentile(50'th) (ms)", timeNanosPercentileCalculator.estimatePercentile(50)/1e6);
        metrics.put("Percentile(75'th) (ms)", timeNanosPercentileCalculator.estimatePercentile(75)/1e6);
        metrics.put("Percentile(90'th) (ms)", timeNanosPercentileCalculator.estimatePercentile(90)/1e6);
        metrics.put("Percentile(95'th) (ms)", timeNanosPercentileCalculator.estimatePercentile(95)/1e6);
        metrics.put("Percentile(99'th) (ms)", timeNanosPercentileCalculator.estimatePercentile(99)/1e6);

        return metrics;
    }

    public void addNewDataPoint(boolean succeeded, boolean invalidPayload, long timeTakenNanos) {
        lock.lock();

        try {
            totalCount++;

            if (succeeded) {
                avgTimeNanos = ((avgTimeNanos * succeededCount) + timeTakenNanos) / ++succeededCount;

                totalQueryTimeNanos += timeTakenNanos;
                queriesPerSecond = (double) succeededCount / (totalQueryTimeNanos / 1e9);

                if (timeTakenNanos < minTimeNanos) {
                    minTimeNanos = timeTakenNanos;
                }

                if (timeTakenNanos > maxTimeNanos) {
                    maxTimeNanos = timeTakenNanos;
                }

                timeNanosPercentileCalculator.addValue(timeTakenNanos);
                timeNanosMedianCalculator.addValue(timeTakenNanos);

                sumOfSquaredDifferences += Math.pow(timeTakenNanos - avgTimeNanos, 2);

                stdDevTimeNanos = Math.sqrt(sumOfSquaredDifferences / succeededCount);
            } else {
                if (invalidPayload) {
                    invalidPayloadCount++;
                } else {
                    failedCount++;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "RuntimeAggregateStats{" +
                "avgTimeMs=" + avgTimeNanos /1e6 +
                ", maxTimeMs=" + maxTimeNanos /1e6 +
                ", minTimeMs=" + minTimeNanos /1e6 +
                ", totalCount=" + totalCount +
                ", queriesPerSecond=" + queriesPerSecond +
                ", succeededCount=" + succeededCount +
                ", failedCount=" + failedCount +
                ", percentile(25)=" + timeNanosPercentileCalculator.estimatePercentile(25)/1e6 +
                ", percentile(50)=" + timeNanosPercentileCalculator.estimatePercentile(50)/1e6 +
                ", percentile(75)=" + timeNanosPercentileCalculator.estimatePercentile(75)/1e6 +
                ", percentile(90)=" + timeNanosPercentileCalculator.estimatePercentile(90)/1e6 +
                ", percentile(95)=" + timeNanosPercentileCalculator.estimatePercentile(95)/1e6 +
                ", percentile(99)=" + timeNanosPercentileCalculator.estimatePercentile(99)/1e6 +
                ", medianTimeMs=" + timeNanosMedianCalculator.getMedian()/1e6 +
                ", stdDevTimeMs=" + stdDevTimeNanos /1e6 +
                '}';
    }
}
