package com.freakynit.sql.db.stress.bench.runner;

import com.freakynit.sql.db.stress.bench.RunningStats;
import com.freakynit.sql.db.stress.bench.configs.QueryPayload;
import com.freakynit.sql.db.stress.bench.configs.BenchmarkingSpec;
import com.freakynit.sql.db.stress.bench.configs.RampUpConfig;
import com.freakynit.sql.db.stress.bench.db.DatabaseInterface;
import com.freakynit.sql.db.stress.bench.utils.TemplateEngine;
import com.freakynit.sql.db.stress.bench.utils.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class BenchmarksRunner {
    public static final Logger logger = LoggerFactory.getLogger(BenchmarksRunner.class);
    private static final String[] OUT_CSV_HEADER = new String[]{"query", "completed", "time_ms", "result_row_count"};

    private final BenchmarkingSpec benchmarkingSpec;
    private final DatabaseInterface databaseInterface;
    private final List<QueryPayload> queryPayloads;

    private List<DataContainer> dataContainers;
    private Map<String, RunningStats> runningStats;

    private AtomicBoolean shouldStop = new AtomicBoolean(false);
    private static AtomicBoolean isRunning = new AtomicBoolean(false);

    public BenchmarksRunner(BenchmarkingSpec benchmarkingSpec, DatabaseInterface databaseInterface, List<QueryPayload> queryPayloads) {
        this.benchmarkingSpec = benchmarkingSpec;
        this.databaseInterface = databaseInterface;
        this.queryPayloads = queryPayloads;
    }

    public BenchmarksRunner prepare() throws IOException {
        if(this.queryPayloads.isEmpty()) {
            throw new IllegalArgumentException("No queries found");
        }

        RampUpConfig rampUpConfig = benchmarkingSpec.getRampUpConfig();
        if(rampUpConfig.getEnable()) {
            if(rampUpConfig.getDurationInSeconds() > (benchmarkingSpec.getRunTimeSeconds() - 5)) {
                throw new IllegalArgumentException("Ramp-up duration should be at least 5 seconds less than `runTimeSeconds`");
            }
        }

        this.dataContainers = DataContainerUtils.initDataContainers(this.queryPayloads);

        if(dataContainers.size() != this.queryPayloads.size()) {
            throw new IllegalArgumentException(String.format("Number of test data sets do not match given number of test queries. Number of given queries: %d, number of test data sets: %d", this.queryPayloads.size(), dataContainers.size()));
        }

        this.runningStats = dataContainers.stream().collect(Collectors.toMap(
                dataContainer -> dataContainer.getQueryPayload().getName(),
                dataContainer -> new RunningStats()
        ));

        return this;
    }

    public void run() throws Exception {
        if(!dataContainers.isEmpty()) {
            isRunning.set(true);

            ExecutorService executorService = null;
            CSVPrinter csvPrinter = null;
            List<Object> connectionObjects = new LinkedList<>();

            try {
                if(benchmarkingSpec.getUseVirtualThreads()) {
                    final ThreadFactory factory = Thread.ofVirtual().name("virtual-thread-", 0).factory();
                    executorService = Executors.newThreadPerTaskExecutor(factory);
                } else {
                    executorService = Executors.newFixedThreadPool(benchmarkingSpec.getConcurrency());
                }

                csvPrinter = benchmarkingSpec.getResultsOutFile() != null ? new CSVPrinter(new FileWriter(benchmarkingSpec.getResultsOutFile()), CSVFormat.DEFAULT.builder().setHeader(OUT_CSV_HEADER).setAutoFlush(true).build()) : null;

                submitJobs(executorService, csvPrinter, connectionObjects);
            } finally {
                if(executorService != null) {
                    Utils.awaitExecutorShutdown(executorService, "benchmarks-runner", false);
                }

                connectionObjects.forEach(databaseInterface::closeConnectionObject);

                if (csvPrinter != null) {
                    csvPrinter.flush();
                    csvPrinter.close();
                }

                isRunning.set(false);
            }
        }
    }

    public void submitJobs(ExecutorService executorService, CSVPrinter csvPrinter, List<Object> connectionObjects) throws InterruptedException {
        RampUpConfig rampUpConfig = benchmarkingSpec.getRampUpConfig();

        long perRampStepSleepMs = rampUpConfig.getEnable()
                ? Float.valueOf(((float)rampUpConfig.getDurationInSeconds()/benchmarkingSpec.getConcurrency() * 1000)).intValue()
                : -1;

        while(connectionObjects.size() < benchmarkingSpec.getConcurrency()) {
            try {
                Object connectionObject = databaseInterface.getConnection();

                final CSVPrinter finalCsvPrinter = csvPrinter;    // needed due to stupid lambada scoping rule •`_´•
                executorService.submit(() -> executeQueries(dataContainers, connectionObject, benchmarkingSpec.getRunTimeSeconds(), finalCsvPrinter, runningStats, benchmarkingSpec.getDefaultQueryTimeoutSeconds()));

                connectionObjects.add(connectionObject);
            } catch (SQLException e) {
                logger.error("Exception getting connection", e);
                System.exit(1);
            } catch (Exception e) {
                logger.error("Exception running benchmark", e);
                System.exit(1);
            }

            if(perRampStepSleepMs > -1) {
                Thread.currentThread().sleep(perRampStepSleepMs);
            }
        }
    }

    public Map<String, RunningStats> getRunningStats() {
        return this.runningStats;
    }

    public void stop() {
        shouldStop.set(true);
        isRunning.set(false);
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    private void executeQueries(List<DataContainer> dataContainers,
                                Object connectionObject,
                                int runTimeSeconds,
                                CSVPrinter csvPrinter,  // important: this could be null
                                Map<String, RunningStats> runningStats,
                                int defaultTimeoutSeconds) {
        List<RunTimeDataContainer> runTimeDataContainers = dataContainers.stream().map(dc -> new RunTimeDataContainer(dc)).collect(Collectors.toList());

        List<String> colValues = null;
        HashMap<String, String> templateVariables = null;
        String query = null;
        long queryStartTimeNanos = 0, timeTakenNanos = 0;
        int resultSetRowCount = 0;
        QueryPayload queryPayload = null;

        long startTime = System.currentTimeMillis();

        while(!shouldStop.get()) {
            for (RunTimeDataContainer runTimeDataContainer : runTimeDataContainers) {
                queryPayload = runTimeDataContainer.getDataContainer().getQueryPayload();

                colValues = runTimeDataContainer.getDataContainer().getDataRows().get(runTimeDataContainer.curIndex % runTimeDataContainer.numRows);
                runTimeDataContainer.curIndex += 1;

                if(colValues == null || queryPayload.getColumns().size() != colValues.size()) {
                    runningStats.get(queryPayload.getName()).addNewDataPoint(false, true, 0);
                } else {
                    templateVariables = new HashMap<>();

                    for (int i = 0; i < queryPayload.getColumns().size(); i++) {
                        templateVariables.put(queryPayload.getColumns().get(i), colValues.get(i));
                    }

                    query = TemplateEngine.render(queryPayload.getQuery(), templateVariables);

                    try {
                        queryStartTimeNanos = System.nanoTime();
                        resultSetRowCount = databaseInterface.executeQuery(
                                connectionObject,
                                query,
                                queryPayload.getConsumeResultSet(),
                                queryPayload.getTimeoutSeconds() != null
                                        ? queryPayload.getTimeoutSeconds()
                                        : defaultTimeoutSeconds);

                        timeTakenNanos = System.nanoTime() - queryStartTimeNanos;
                        runningStats.get(queryPayload.getName()).addNewDataPoint(true, false, timeTakenNanos);

                        if(csvPrinter != null) {
                            csvPrinter.printRecord(queryPayload.getName(), true, timeTakenNanos/1_000_000, resultSetRowCount);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        runningStats.get(queryPayload.getName()).addNewDataPoint(false, false, 0);

                        if(csvPrinter != null) {
                            try {
                                csvPrinter.printRecord(queryPayload.getName(), false, 0, 0);
                            } catch (IOException ex) {
                                logger.error("Exception writing stat to csv file", e);
                            }
                        }
                    }
                }
            }

            //logger.info("time lapsed: {}, runTimeSeconds: {}", (System.currentTimeMillis() - startTime)/1000, runTimeSeconds);
            if((System.currentTimeMillis() - startTime)/1000 > runTimeSeconds) {
                break;
            }
        }
    }
}
