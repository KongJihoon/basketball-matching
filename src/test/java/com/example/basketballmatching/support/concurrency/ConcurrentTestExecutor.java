package com.example.basketballmatching.support.concurrency;

import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class ConcurrentTestExecutor {

    private static final Duration READY_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(30);

    private ConcurrentTestExecutor() {}

    public static <T> ConcurrentTestResult execute(List<T> targets, Consumer<T> task) throws InterruptedException {

        int taskCount = targets.size();

        ExecutorService executorService = Executors.newFixedThreadPool(taskCount);

        CountDownLatch readyLatch = new CountDownLatch(taskCount);

        CountDownLatch startLatch = new CountDownLatch(1);

        CountDownLatch doneLatch = new CountDownLatch(taskCount);

        AtomicInteger successCount = new AtomicInteger();

        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (T target : targets) {

                executorService.submit(() -> {
                    readyLatch.countDown();

                    try {
                        startLatch.await();
                        task.accept(target);

                        successCount.incrementAndGet();
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    } finally {
                        doneLatch.countDown();
                    }
                });

            }
            boolean allTaskReady = readyLatch.await(READY_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            if (!allTaskReady) {
                throw new IllegalStateException("동시성 테스트 작업 준비 시간 초과");
            }

            long startTime = System.currentTimeMillis();

            startLatch.countDown();

            boolean completed = doneLatch.await(
                    EXECUTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS
            );

            if (!completed) {
                throw new IllegalStateException("동시성 테스트 실행 시간 초과");
            }

            long executionTimeMillis =
                    System.currentTimeMillis() - startTime;

            return new ConcurrentTestResult(
                    taskCount,
                    successCount.get(),
                    failures.size(),
                    executionTimeMillis,
                    List.copyOf(failures)
            );


        } finally {
            executorService.shutdown();
        }


    }

    public record ConcurrentTestResult(
            int totalCount,
            int successCount,
            int failureCount,
            long executionTimeMillis,
            List<Throwable> failures
    ) {
    }

}
