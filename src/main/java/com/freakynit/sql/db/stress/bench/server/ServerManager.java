package com.freakynit.sql.db.stress.bench.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freakynit.sql.db.stress.bench.StatsProviderInterface;
import com.freakynit.sql.db.stress.bench.configs.ServerConfig;
import com.freakynit.sql.db.stress.bench.model.ApiResponseContainer;
import com.freakynit.sql.db.stress.bench.model.StatsApiResponseModel;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.rendering.template.JavalinMustache;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ServerManager {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Javalin server;
    private static StatsProviderInterface statsProvider;

    public static void start(ServerConfig serverConfig, StatsProviderInterface statsProvider) {
        ServerManager.statsProvider = statsProvider;

        server = Javalin.create(config -> {
                    config.staticFiles.add("/views");
                    config.fileRenderer(new JavalinMustache());
                })
                .get("/", ctx -> ctx.render("/views/index.html", Map.of("updateIntervalSeconds", serverConfig.getGraphUpdateIntervalSeconds())))
                .get("/stats", new GetStatsHandler());

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
            ctx.json(mapper.writeValueAsString(
                    new ApiResponseContainer<StatsApiResponseModel>(true, null, statsProvider.getSnapshot())
            ));
        }
    }
}
