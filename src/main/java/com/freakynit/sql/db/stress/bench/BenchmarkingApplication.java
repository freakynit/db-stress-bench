package com.freakynit.sql.db.stress.bench;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.configs.Config;
import com.freakynit.sql.db.stress.bench.configs.DBConfig;
import com.freakynit.sql.db.stress.bench.db.DatabaseInterface;
import com.freakynit.sql.db.stress.bench.runner.BenchmarksRunner;
import com.freakynit.sql.db.stress.bench.server.ServerManager;
import com.freakynit.sql.db.stress.bench.utils.StatsUtils;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class BenchmarkingApplication {
    public static final Logger logger = LoggerFactory.getLogger(BenchmarkingApplication.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static Config config;
    private static DatabaseInterface databaseInterface;
    private static BenchmarksRunner benchmarksRunner;
    private static ScheduledExecutorService scheduledStatsLogger;

    public static void main(String[] args) throws Throwable {
        config = new Yaml().loadAs(new FileReader("config.yaml"), Config.class);

        DBConfig dbConfig = config.getDbConfigs().stream()
                .filter(c -> c.getImpl().equals(config.getBenchmarkingSpec().getActiveDatabase()))
                .findFirst()
                .orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Configuration for database: " + config.getBenchmarkingSpec().getActiveDatabase() + " not found"));

        databaseInterface = initDatabaseInterface(dbConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownServices()));

        // Create a PrometheusMeterRegistry (this is the registry that collects metrics)
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT);

        // Register the registry with Micrometer
        Metrics.addRegistry(prometheusRegistry);

        runBenchmarkingIteration();
    }

    private static void runBenchmarkingIteration() throws Exception {
        DBConfig dbConfig = config.getDbConfigs().stream()
                .filter(c -> c.getImpl().equals(config.getBenchmarkingSpec().getActiveDatabase()))
                .findFirst().orElse(null);  // this should not be null at this point since we have performed validation on application start itself

        benchmarksRunner = new BenchmarksRunner(config.getBenchmarkingSpec(), databaseInterface, dbConfig.getQueryPayloads());
        benchmarksRunner.prepare();

        scheduledStatsLogger = startStdoutPrinterIfNeeded();

        if(config.getServerConfig().getEnableStatsEndpoint() || config.getServerConfig().getEnablePrometheusMetricsEndpoint()) {
            ServerManager.start(config.getServerConfig(), benchmarksRunner);
            logger.info("Started running on port: {}", config.getServerConfig().getPort());

            if(config.getServerConfig().getEnableStatsEndpoint()) {
                logger.info("You can see running stats here: http://localhost:{}", config.getServerConfig().getPort());
            }
        }

        benchmarksRunner.run();

        shutdownServices();
    }

    private static DatabaseInterface initDatabaseInterface(DBConfig dbConfig) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = Class.forName(dbConfig.getImpl());
        if (!DatabaseInterface.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(dbConfig.getImpl() + " does not implement DatabaseInterface");
        }

        return (DatabaseInterface)clazz.getDeclaredConstructor(Map.class).newInstance(dbConfig.getProperties());
    }

    // Note: all services used here should be idempotent on stop()/shutdown()
    private static void shutdownServices() {
        try {
            logger.info("\n=== Final Benchmark Stats ===");
            logger.info(mapper.writeValueAsString(StatsUtils.toMap(benchmarksRunner.getRunningStats())));
        } catch (JsonProcessingException ignored) {}

        ServerManager.stop();

        if(benchmarksRunner != null) {
            benchmarksRunner.stop();
        }

        if(scheduledStatsLogger != null) {
            scheduledStatsLogger.shutdown();
        }
    }

    private static ScheduledExecutorService startStdoutPrinterIfNeeded() {
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if(benchmarksRunner.isRunning()) {
                try {
                    logger.info(mapper.writeValueAsString(StatsUtils.toMap(benchmarksRunner.getRunningStats())));
                } catch (JsonProcessingException ignored) {
                }
            } else {
                logger.info("Waiting for benchmarking to begin..");
            }
        }, 0, config.getBenchmarkingSpec().getPrintRunningStatsEveryNSeconds(), TimeUnit.SECONDS);

        return scheduledExecutor;
    }
}
