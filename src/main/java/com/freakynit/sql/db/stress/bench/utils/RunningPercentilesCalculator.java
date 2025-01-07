package com.freakynit.sql.db.stress.bench.utils;

import java.util.Random;

public class RunningPercentilesCalculator {
    private double[] buckets;
    private double bucketSize;
    private double minValue;
    private double maxValue;
    private int totalCount;

    // try to keep diff between initialMax and initialMin small initially to not lose unnecessary precision
    public RunningPercentilesCalculator(double initialMin, double initialMax, int initialBuckets) {
        this.minValue = initialMin;
        this.maxValue = initialMax;
        this.bucketSize = (maxValue - minValue) / initialBuckets;
        this.buckets = new double[initialBuckets];
        this.totalCount = 0;
    }

    private void expandRange(double newValue) {
        double newMin = Math.min(minValue, newValue);
        double newMax = Math.max(maxValue, newValue);
        double newBucketSize = (newMax - newMin) / buckets.length;

        double[] newBuckets = new double[buckets.length];

        for (int i = 0; i < buckets.length; i++) {
            double bucketStart = minValue + i * bucketSize;
            double bucketEnd = bucketStart + bucketSize;

            // Calculate starting index and the number of buckets to distribute the count to
            int newStartIndex = (int)Math.floor((bucketStart - newMin) / newBucketSize);
            int newEndIndex = (int) Math.floor((bucketEnd - newMin) / newBucketSize);

            if (newStartIndex == newEndIndex) { // if bucket overlaps 1 bucket
                newBuckets[newStartIndex] += buckets[i];
            }
            else { // if bucket overlaps multiple buckets
                double range = bucketEnd - bucketStart;
                for(int j = newStartIndex; j < newEndIndex; j++){
                    double newStart = newMin + (j * newBucketSize);
                    double newEnd = newMin + ((j + 1) * newBucketSize);

                    double overlap = Math.min(bucketEnd, newEnd) - Math.max(bucketStart, newStart);
                    newBuckets[j] += buckets[i] * (overlap / range);
                }
            }

        }

        this.minValue = newMin;
        this.maxValue = newMax;
        this.bucketSize = newBucketSize;
        this.buckets = newBuckets;
    }

    public void addValue(double value) {
        if (value < minValue || value > maxValue) {
            expandRange(value);
        }
        int bucketIndex = Math.min((int)((value - minValue) / bucketSize), buckets.length - 1);
        buckets[bucketIndex]++;
        totalCount++;
    }


    public double estimatePercentile(double percentile) {
        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100");
        }

        int targetRank = (int) Math.ceil((percentile / 100) * totalCount);
        double cumulativeCount = 0;

        for (int i = 0; i < buckets.length; i++) {
            cumulativeCount += buckets[i];
            if (cumulativeCount >= targetRank) {
                double bucketStart = minValue + i * bucketSize;
                double bucketEnd = bucketStart + bucketSize;
                double bucketFraction = (double) (targetRank - (cumulativeCount - buckets[i])) / buckets[i];
                return bucketStart + bucketFraction * bucketSize;
            }
        }

        return maxValue;
    }

    public static void main(String[] args) {
        benchmark();
    }

    public static void correctnessChecks() {
        RunningPercentilesCalculator estimator = new RunningPercentilesCalculator(0, 100, 100);

        double[] data = {10.0, 200.0, 15.0, 30.0, 50.0, 70.0, 850.0, 900.0, 95.0, 999.0};
        for (double value : data) {
            estimator.addValue(value);
        }

        System.out.println("25th Percentile: " + estimator.estimatePercentile(25));
        System.out.println("50th Percentile: " + estimator.estimatePercentile(50));
        System.out.println("75th Percentile: " + estimator.estimatePercentile(75));
        System.out.println("95th Percentile: " + estimator.estimatePercentile(95));
    }

    private static void benchmark(){
        // Parameters for benchmarking
        int numValues = 1_000_0000; // Number of addValue calls
        int minValue = 0; // Starting minimum range
        int numBuckets = 10; // Initial number of buckets
        int maxValue = minValue + 10;

        // Range expansion points calculation
        int maxInt = Integer.MAX_VALUE;
        double numExpansionSlots = Math.log(maxInt) / Math.log(maxValue);
        int nextRangeExpansionAt = Double.valueOf(numValues / numExpansionSlots).intValue();

        // Initialize the calculator
        RunningPercentilesCalculator estimator = new RunningPercentilesCalculator(minValue, maxValue, numBuckets);

        // Generate random data
        Random random = new Random();
        int[] randomData = new int[numValues];
        for (int i = 0; i < numValues; i++) {
            if(i >= nextRangeExpansionAt) {
                nextRangeExpansionAt *= 2;
                maxValue *= maxValue;
            }
            randomData[i] = random.nextInt(maxValue);
        }

        long startTime = System.nanoTime();
        for (int value : randomData) {
            estimator.addValue(value);
        }
        long endTime = System.nanoTime();

        double timeInMilliseconds = (endTime - startTime) / 1_000_000.0;

        System.out.printf("Benchmark for %d addValue calls:%n", numValues);
        System.out.printf("Total time: %.2f ms%n", timeInMilliseconds);
        System.out.printf("Average time per addValue call: %.6f ms%n", timeInMilliseconds / numValues);

        System.out.println("25th Percentile: " + estimator.estimatePercentile(25));
        System.out.println("50th Percentile: " + estimator.estimatePercentile(50));
        System.out.println("75th Percentile: " + estimator.estimatePercentile(75));
        System.out.println("95th Percentile: " + estimator.estimatePercentile(95));
    }
}