package com.freakynit.sql.db.stress.bench.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static void awaitExecutorShutdown(ExecutorService executorService, String executorName, boolean logAwaitingShutdownMessage) {
        executorService.shutdown();
        try {
            while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                if(logAwaitingShutdownMessage) {
                    logger.info("Awaiting executor: {} shutdown...", executorName);
                }
            }
            logger.info("All tasks of executor: {} completed", executorName);
        } catch (InterruptedException e) {
            logger.info("Force-stopping running tasks of executor: {}...", executorName);
            executorService.shutdownNow();
        }
    }
}
