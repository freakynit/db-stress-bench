package com.freakynit.sql.db.stress.bench.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.configs.ServerConfig;
import com.freakynit.sql.db.stress.bench.model.ApiResponseContainer;
import com.freakynit.sql.db.stress.bench.model.StatsApiResponseModel;
import com.freakynit.sql.db.stress.bench.runner.BenchmarksRunner;
import com.freakynit.sql.db.stress.bench.utils.StatsUtils;
import com.freakynit.sql.db.stress.bench.utils.backports.MapUtils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class ServerManager {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static Javalin server;
    private static BenchmarksRunner benchmarksRunner;

    public static void start(ServerConfig serverConfig, BenchmarksRunner benchmarksRunner) {
        ServerManager.benchmarksRunner = benchmarksRunner;

        server = Javalin.create(config -> config.addStaticFiles("/views", Location.CLASSPATH));

        if(serverConfig.getEnableStatsEndpoint()) {
            server = server
                    .get("/", ctx -> ctx.render("/views/index.html", MapUtils.mapOf("updateIntervalSeconds", 2)))
                    .get("/stats", new GetStatsHandler());
        }

        if(serverConfig.getEnablePrometheusMetricsEndpoint()) {
            server = server.get("/metrics", new PrometheusMetricsHandler());
        }

        new Thread(() -> server.start(serverConfig.getPort())).start();
    }

    public static void stop() {
        if(server != null) {
            server.stop();
        }
    }

    private static class GetStatsHandler implements Handler {
        @Override
        public void handle(@NotNull Context ctx) throws Exception {
            StatsApiResponseModel statsApiResponseModel = new StatsApiResponseModel(benchmarksRunner.isRunning(), new Date().getTime(), StatsUtils.toMap(benchmarksRunner.getRunningStats()));
            ctx.json(mapper.writeValueAsString(new ApiResponseContainer<>(true, null, statsApiResponseModel)));
        }
    }

    private static class PrometheusMetricsHandler implements Handler {
        @Override
        public void handle(@NotNull Context ctx) throws Exception {
            ctx.contentType("text/plain");
            ctx.result(StatsUtils.getPrometheusScrape(benchmarksRunner.getRunningStats()));
        }
    }
}
