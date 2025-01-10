package com.freakynit.sql.db.stress.bench.utils;

import com.freakynit.sql.db.stress.bench.RunningStats;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsUtils {
    public static Map<String, Map<String, Number>> toMap(Map<String, RunningStats> runningStats) {
        return runningStats == null
                ? Collections.emptyMap()
                : runningStats.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toMap()));
    }

    public static String getPrometheusScrape(Map<String, RunningStats> runningStats) {
        Map<String, Map<String, Number>> metricsMap = toMap(runningStats);

        StringBuilder metrics = new StringBuilder();

        for (Map.Entry<String, Map<String, Number>> entry : metricsMap.entrySet()) {
            String queryName = entry.getKey();

            for (Map.Entry<String, Number> metricEntry : entry.getValue().entrySet()) {
                String metricType = RunningStats.COUNTER_METRICS.contains(metricEntry.getKey()) ? "counter" : "gauge";

                String metricName = toPrometheusSafeName(metricEntry.getKey());
                Number metricValue = metricEntry.getValue();

                // Generate Prometheus-compatible format
                metrics.append(String.format("# HELP %s Description for %s\n", metricName, metricName));
                metrics.append(String.format("# TYPE %s %s\n", metricName, metricType));
                metrics.append(String.format("%s{query_name=\"%s\"} %s\n", metricName, queryName, metricValue));
            }
        }

        return metrics.toString();
    }

    private static String toPrometheusSafeName(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input
                .replaceAll("[\\s\\(\\)\\[\\]']", "_")  // Replace spaces, parentheses, brackets, and single quotes with underscores
                .replaceAll("[^a-zA-Z0-9_]", "")        // Remove non-alphanumeric characters except underscores
                .toLowerCase()                                            // Convert to lowercase
                .replaceAll("_+", "_")                  // Replace multiple consecutive underscores with a single underscore
                .replaceAll("^_+|_+$", "");             // Remove leading or trailing underscores if they exist
    }
}
