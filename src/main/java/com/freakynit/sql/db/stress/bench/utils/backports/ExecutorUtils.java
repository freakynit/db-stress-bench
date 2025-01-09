package com.freakynit.sql.db.stress.bench.utils.backports;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

public class ExecutorUtils {
    public static ExecutorService createExecutorService(Boolean useVirtualThreads, Integer optionalThreadCount) {
        if (useVirtualThreads) {
            // Try to create a virtual thread executor using reflection
            return createVirtualThreadExecutor()
                    .orElseThrow(() -> new UnsupportedOperationException(
                            "Virtual threads are not supported on this JVM version."));
        } else {
            // Fallback to a fixed thread pool for native threads
            return Executors.newFixedThreadPool(optionalThreadCount.intValue());
        }
    }

    private static Supplier<Optional<ExecutorService>> virtualThreadExecutorSupplier = () -> {
        try {
            // Use reflection to check if Thread.ofVirtual exists
            Class<?> threadClass = Class.forName("java.lang.Thread");
            Class<?> builderClass = Class.forName("java.lang.Thread$Builder");

            // Get the ofVirtual() method and factory() method
            Object virtualThreadBuilder = threadClass.getMethod("ofVirtual").invoke(null);
            ThreadFactory factory = (ThreadFactory) builderClass.getMethod("factory").invoke(virtualThreadBuilder);

            Class<?> executorsClass = Executors.class;
            Method method = executorsClass.getMethod("newThreadPerTaskExecutor", ThreadFactory.class);

            ExecutorService executorService = (ExecutorService) method.invoke(null, factory);
            return Optional.of(executorService);
        } catch (Exception e) {
            // If any exception occurs, return empty Optional
            return Optional.empty();
        }
    };

    private static Optional<ExecutorService> createVirtualThreadExecutor() {
        return virtualThreadExecutorSupplier.get();
    }
}
