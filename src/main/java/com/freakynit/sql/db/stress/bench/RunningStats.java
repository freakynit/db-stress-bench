package com.freakynit.sql.db.stress.bench;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.freakynit.sql.db.stress.bench.deserializers.MedianStatJsonSerializer;
import com.freakynit.sql.db.stress.bench.deserializers.PercentileStatsJsonSerializer;
import com.freakynit.sql.db.stress.bench.utils.RunningPercentilesCalculator;
import com.freakynit.sql.db.stress.bench.utils.RunningMedianCalculator;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class RunningStats {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String METRIC_QUERIES_PER_SECOND = "Queries Per Second (completed only)";
    private static final String METRIC_AVERAGE_QUERY_TIME_MS = "Average Query Time (ms)";
    private static final String METRIC_QUERY_STDDEV_MS = "Query StdDev (ms)";
    private static final String METRIC_MIN_QUERY_TIME_MS = "Min Query Time (ms)";
    private static final String METRIC_MAX_QUERY_TIME_MS = "Max Query Time (ms)";
    private static final String METRIC_MEDIAN_QUERY_TIME_MS = "Median Query Time (ms)";
    private static final String METRIC_SUCCEEDED_QUERIES = "Successfully Completed Queries";
    private static final String METRIC_FAILED_QUERIES = "Failed Queries";
    private static final String METRIC_INVALID_INPUT_PAYLOADS = "Invalid Input Payloads";
    private static final String METRIC_TOTAL_QUERIES = "Total Queries (completed + failed + invalid payloads)";
    private static final String METRIC_PERCENTILE_20_MS = "Percentile(20'th) (ms)";
    private static final String METRIC_PERCENTILE_50_MS = "Percentile(50'th) (ms)";
    private static final String METRIC_PERCENTILE_75_MS = "Percentile(75'th) (ms)";
    private static final String METRIC_PERCENTILE_90_MS = "Percentile(90'th) (ms)";
    private static final String METRIC_PERCENTILE_95_MS = "Percentile(95'th) (ms)";
    private static final String METRIC_PERCENTILE_99_MS = "Percentile(99'th) (ms)";

    public static final List<String> COUNTER_METRICS = Arrays.asList(METRIC_SUCCEEDED_QUERIES, METRIC_FAILED_QUERIES, METRIC_INVALID_INPUT_PAYLOADS, METRIC_TOTAL_QUERIES);

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

        metrics.put(METRIC_QUERIES_PER_SECOND, queriesPerSecond);
        metrics.put(METRIC_AVERAGE_QUERY_TIME_MS, avgTimeNanos / 1e6);
        metrics.put(METRIC_QUERY_STDDEV_MS, stdDevTimeNanos / 1e6);
        metrics.put(METRIC_MIN_QUERY_TIME_MS, minTimeNanos / 1e6);
        metrics.put(METRIC_MAX_QUERY_TIME_MS, maxTimeNanos / 1e6);
        metrics.put(METRIC_MEDIAN_QUERY_TIME_MS, timeNanosMedianCalculator.getMedian() / 1e6);
        metrics.put(METRIC_SUCCEEDED_QUERIES, succeededCount);
        metrics.put(METRIC_FAILED_QUERIES, failedCount);
        metrics.put(METRIC_INVALID_INPUT_PAYLOADS, invalidPayloadCount);
        metrics.put(METRIC_TOTAL_QUERIES, totalCount);
        metrics.put(METRIC_PERCENTILE_20_MS, timeNanosPercentileCalculator.estimatePercentile(20) / 1e6);
        metrics.put(METRIC_PERCENTILE_50_MS, timeNanosPercentileCalculator.estimatePercentile(50) / 1e6);
        metrics.put(METRIC_PERCENTILE_75_MS, timeNanosPercentileCalculator.estimatePercentile(75) / 1e6);
        metrics.put(METRIC_PERCENTILE_90_MS, timeNanosPercentileCalculator.estimatePercentile(90) / 1e6);
        metrics.put(METRIC_PERCENTILE_95_MS, timeNanosPercentileCalculator.estimatePercentile(95) / 1e6);
        metrics.put(METRIC_PERCENTILE_99_MS, timeNanosPercentileCalculator.estimatePercentile(99) / 1e6);

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
        try {
            return mapper.writeValueAsString(toMap());
        } catch (JsonProcessingException e) {
            return toMap().toString();
        }
    }
}
