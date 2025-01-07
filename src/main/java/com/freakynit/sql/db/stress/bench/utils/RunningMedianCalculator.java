package com.freakynit.sql.db.stress.bench.utils;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Random;

public class RunningMedianCalculator {
    // Max-heap to store the smaller half of the data
    private PriorityQueue<Double> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    // Min-heap to store the larger half of the data
    private PriorityQueue<Double> minHeap = new PriorityQueue<>();

    public void addValue(double value) {
        // Add to the appropriate heap
        if (maxHeap.isEmpty() || value <= maxHeap.peek()) {
            maxHeap.add(value);
        } else {
            minHeap.add(value);
        }

        // Balance the heaps if their sizes differ by more than 1
        if (maxHeap.size() > minHeap.size() + 1) {
            minHeap.add(maxHeap.poll());
        } else if (minHeap.size() > maxHeap.size()) {
            maxHeap.add(minHeap.poll());
        }
    }

    public double getMedian() {
        if(maxHeap.size() == 0) {
            return 0.0;
        }

        if (maxHeap.size() == minHeap.size()) {
            // If even number of elements, median is the average of the two middle values
            return (maxHeap.peek() + minHeap.peek()) / 2.0;
        } else {
            // If odd, median is the top of the max-heap
            return maxHeap.peek();
        }
    }

    public static void main(String[] args) {
        benchmark();
    }

    private static void correctnessChecks() {
        RunningMedianCalculator calculator = new RunningMedianCalculator();

        double[] data = {5, 15, 1, 3, 8, 7};
        for (double value : data) {
            calculator.addValue(value);
            System.out.println("Added: " + value + ", Current Median: " + calculator.getMedian());
        }
    }

    private static void benchmark() {
        RunningMedianCalculator calculator = new RunningMedianCalculator();
        Random random = new Random();

        long startTime = System.nanoTime();
        for (int i = 0; i < 10_000_000; i++) {
            calculator.addValue(random.nextInt(1000000));
        }
        long endTime = System.nanoTime();

        double timeInMilliseconds = (endTime - startTime) / 1_000_000.0;

        System.out.println("median: " + calculator.getMedian() + ", timeInMilliseconds: " + timeInMilliseconds);
    }
}
