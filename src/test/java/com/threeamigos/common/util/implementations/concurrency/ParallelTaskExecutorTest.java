package com.threeamigos.common.util.implementations.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParallelTaskExecutor Tests")
class ParallelTaskExecutorTest {

    @Test
    @DisplayName("Should keep track of thread pool size")
    void shouldKeepTrackOfThreadPoolSize() throws InterruptedException {
        // Given
        try (ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2)) {
            // When
            int poolSize = executor.getPlatformThreadPoolSize();
            // Then
            assertEquals(2, poolSize);
        }
    }
    
    @Test
    @DisplayName("Should execute tasks in parallel with platform threads")
    void shouldExecuteTasksInParallelWithPlatformThreads() throws InterruptedException {
        // Given
        try (ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(4)) {
            AtomicInteger counter = new AtomicInteger(0);
            int taskCount = 100;

            // When
            for (int i = 0; i < taskCount; i++) {
                executor.submit(counter::incrementAndGet);
            }
            executor.awaitCompletion();

            // Then
            assertEquals(taskCount, counter.get());
            assertEquals(0, executor.getPendingTaskCount());

            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should execute tasks with virtual threads when available")
    void shouldExecuteTasksWithVirtualThreads() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor();
        if (!executor.supportsVirtualThreads()) {
            executor.shutdown();
            return; // skip if virtual threads not available
        }

        try {
            AtomicInteger counter = new AtomicInteger(0);
            int taskCount = 200;

            // When
            for (int i = 0; i < taskCount; i++) {
                executor.scheduleVirtualThread(counter::incrementAndGet);
            }
            executor.awaitCompletion();

            // Then
            assertEquals(taskCount, counter.get());
            assertEquals(0, executor.getPendingTaskCount());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should queue tasks and execute when threads available")
    void shouldQueueTasksAndExecuteWhenThreadsAvailable() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(10);
        List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // When - submit 10 tasks with 2 threads
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All tasks wait to start together
                    executionOrder.add(taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all tasks
        executor.awaitCompletion();

        // Then
        assertEquals(10, executionOrder.size());
        assertTrue(finishLatch.await(1, TimeUnit.SECONDS));

        executor.shutdown();
    }

    @Test
    @DisplayName("Should track pending task count correctly")
    void shouldTrackPendingTaskCountCorrectly() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch taskRunningLatch = new CountDownLatch(2);

        // When - submit 5 slow tasks
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    taskRunningLatch.countDown();
                    startLatch.await(); // Block until released
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wait for at least 2 tasks to start
        assertTrue(taskRunningLatch.await(1, TimeUnit.SECONDS));

        // Then - should have 5 pending tasks
        assertEquals(5, executor.getPendingTaskCount());

        // Release tasks and wait
        startLatch.countDown();
        executor.awaitCompletion();

        // Should have 0 pending tasks
        assertEquals(0, executor.getPendingTaskCount());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should wait for completion with timeout")
    void shouldWaitForCompletionWithTimeout() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(1);
        CountDownLatch blockLatch = new CountDownLatch(1);

        try {
            // When - submit a long-running task
            executor.submit(() -> {
                try {
                    blockLatch.await(); // Block indefinitely
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Then - timeout should occur
            boolean completed = executor.awaitCompletion(100, TimeUnit.MILLISECONDS);
            assertFalse(completed, "Should timeout before task completes");

            // Cleanup - release the blocked task
            blockLatch.countDown();
            executor.awaitCompletion();
        } finally {
            executor.shutdownNow(); // Force shutdown to ensure cleanup
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should shutdown gracefully")
    void shouldShutdownGracefully() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);

        // When
        for (int i = 0; i < 10; i++) {
            executor.schedulePlatformThread(counter::incrementAndGet);
        }
        executor.awaitCompletion();
        executor.shutdown();

        // Then
        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
        assertEquals(10, counter.get());
    }

    @Test
    @DisplayName("Should shutdown immediately and interrupt running tasks")
    void shouldShutdownImmediatelyAndInterruptRunningTasks() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        AtomicInteger interruptedCount = new AtomicInteger(0);
        CountDownLatch tasksStarted = new CountDownLatch(2); // Only wait for 2 tasks (pool size)

        // When - submit 5 tasks that wait indefinitely
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    tasksStarted.countDown();
                    Thread.sleep(10000); // Sleep for long time
                } catch (InterruptedException e) {
                    interruptedCount.incrementAndGet();
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wait for at least 2 tasks to start (the pool size)
        assertTrue(tasksStarted.await(1, TimeUnit.SECONDS));

        // Then - shutdownNow should interrupt tasks
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        assertTrue(interruptedCount.get() > 0, "At least some tasks should be interrupted");
    }

    @Test
    @DisplayName("Should reject tasks after shutdown")
    void shouldRejectTasksAfterShutdown() {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        executor.shutdown();

        // When/Then
        assertThrows(IllegalStateException.class, () -> executor.submit(() -> {}));
    }

    @Test
    @DisplayName("Should handle exceptions in tasks gracefully")
    void shouldHandleExceptionsInTasksGracefully() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - submit mix of successful and failing tasks
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.schedulePlatformThread(() -> {
                if (taskId % 2 == 0) {
                    throw new RuntimeException("Task " + taskId + " failed");
                } else {
                    successCount.incrementAndGet();
                }
            });
        }

        executor.awaitCompletion();

        // Then - successful tasks should complete despite failures
        assertEquals(5, successCount.get());
        assertEquals(0, executor.getPendingTaskCount());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should support AutoCloseable pattern")
    void shouldSupportAutoCloseablePattern() throws Exception {
        // Given
        AtomicInteger counter = new AtomicInteger(0);

        // When
        try (ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2)) {
            for (int i = 0; i < 10; i++) {
                executor.schedulePlatformThread(counter::incrementAndGet);
            }
            executor.awaitCompletion();
        }

        // Then - executor should be closed automatically
        assertEquals(10, counter.get());
    }

    @Test
    @DisplayName("close should trigger shutdown and block new submissions")
    void closeShouldTriggerShutdown() throws Exception {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(1);
        AtomicInteger counter = new AtomicInteger();

        executor.submit(counter::incrementAndGet);

        executor.close();

        assertEquals(1, counter.get());
        assertTrue(executor.isShutdown(), "close should call shutdown");
        assertThrows(IllegalStateException.class, () -> executor.submit(() -> {}));
    }

    @Test
    @DisplayName("Should handle concurrent submissions from multiple threads")
    void shouldHandleConcurrentSubmissionsFromMultipleThreads() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);
        int submitterThreads = 5;
        int tasksPerThread = 20;
        CountDownLatch submissionLatch = new CountDownLatch(submitterThreads);

        // When - submit tasks from multiple threads concurrently
        List<Thread> submitters = new ArrayList<>();
        for (int i = 0; i < submitterThreads; i++) {
            Thread submitter = new Thread(() -> {
                for (int j = 0; j < tasksPerThread; j++) {
                    executor.schedulePlatformThread(counter::incrementAndGet);
                }
                submissionLatch.countDown();
            });
            submitters.add(submitter);
            submitter.start();
        }

        // Wait for all submissions to complete
        assertTrue(submissionLatch.await(5, TimeUnit.SECONDS));

        // Wait for all tasks to execute
        executor.awaitCompletion();

        // Then
        assertEquals(submitterThreads * tasksPerThread, counter.get());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should execute tasks on different threads")
    void shouldExecuteTasksOnDifferentThreads() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(4);
        ConcurrentHashMap<Long, Boolean> threadIds = new ConcurrentHashMap<>();

        // When
        for (int i = 0; i < 20; i++) {
            executor.schedulePlatformThread(() -> threadIds.put(Thread.currentThread().getId(), true));
        }
        executor.awaitCompletion();

        // Then - should use multiple threads
        assertTrue(threadIds.size() > 1, "Should use multiple threads");
        assertTrue(threadIds.size() <= 4, "Should not exceed pool size");

        executor.shutdown();
    }

    @Test
    @DisplayName("Should throw exception for null task")
    void shouldThrowExceptionForNullTask() {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> executor.schedulePlatformThread(null));

        executor.shutdown();
    }

    @Test
    @DisplayName("Should throw exception for invalid thread pool size")
    void shouldThrowExceptionForInvalidThreadPoolSize() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> ParallelTaskExecutor.createExecutor(0));
        assertThrows(IllegalArgumentException.class, () -> ParallelTaskExecutor.createExecutor(-1));
    }

    @Test
    @DisplayName("Should throw exception for invalid timeout")
    void shouldThrowExceptionForInvalidTimeout() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);

        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> executor.awaitCompletion(-1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
            () -> executor.awaitCompletion(1, null));

        executor.shutdown();
    }

    @Test
    @DisplayName("Should report active task count while tasks are running")
    void shouldReportActiveTaskCount() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        executor.schedulePlatformThread(() -> {
            started.countDown();
            awaitSilently(release);
        });
        executor.schedulePlatformThread(() -> {
            started.countDown();
            awaitSilently(release);
        });

        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertEquals(2, executor.getActiveTaskCount());

        release.countDown();
        executor.awaitCompletion();
        assertEquals(0, executor.getActiveTaskCount());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should schedule both virtual and platform tasks")
    void shouldScheduleVirtualAndPlatformTasks() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor();
        AtomicInteger counter = new AtomicInteger();

        executor.scheduleVirtualThread(counter::incrementAndGet);
        executor.schedulePlatformThread(counter::incrementAndGet);

        executor.awaitCompletion();

        assertEquals(2, counter.get());
        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle awaitCompletion when no tasks submitted")
    void shouldHandleAwaitCompletionWhenNoTasksSubmitted() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);

        // When
        executor.awaitCompletion(); // Should return immediately

        // Then
        assertEquals(0, executor.getPendingTaskCount());

        executor.shutdown();
    }

    @Test
    @DisplayName("awaitCompletion(timeout) should return immediately when no tasks are pending")
    void shouldReturnImmediatelyWhenNoTasksWithTimeout() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(1);
        assertTrue(executor.awaitCompletion(100, TimeUnit.MILLISECONDS));
        executor.shutdown();
    }

    @Test
    @DisplayName("schedule should throw when target executor is null")
    void shouldThrowWhenTargetExecutorIsNull() throws Exception {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(1);
        Method schedule = ParallelTaskExecutor.class.getDeclaredMethod("schedule", Runnable.class, ExecutorService.class);
        schedule.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () ->
                schedule.invoke(executor, (Runnable) () -> {}, null));
        assertTrue(ex.getCause() instanceof IllegalStateException);
        executor.shutdown();
    }

    @Test
    @DisplayName("schedule should decrement pending when submit fails")
    void shouldDecrementPendingWhenSubmitFails() throws Exception {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(1);
        ExecutorService failing = new ExecutorService() {
            @Override public void shutdown() {}
            @Override public List<Runnable> shutdownNow() { return Collections.emptyList(); }
            @Override public boolean isShutdown() { return false; }
            @Override public boolean isTerminated() { return false; }
            @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
            @Override public <T> Future<T> submit(Callable<T> task) { throw new RuntimeException("boom"); }
            @Override public <T> Future<T> submit(Runnable task, T result) { throw new RuntimeException("boom"); }
            @Override public Future<?> submit(Runnable task) { throw new RuntimeException("boom"); }
            @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
            @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
            @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
            @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
            @Override public void execute(Runnable command) { throw new UnsupportedOperationException(); }
        };
        Method schedule = ParallelTaskExecutor.class.getDeclaredMethod("schedule", Runnable.class, ExecutorService.class);
        schedule.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> schedule.invoke(executor, (Runnable) () -> {}, failing));
        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals(0, executor.getPendingTaskCount());
        executor.shutdown();
    }

    @Test
    @DisplayName("Should cover branches when virtual executor is absent")
    void shouldCoverBranchWhenVirtualExecutorAbsent() throws Exception {
        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };
        ExecutorService platform = Executors.newSingleThreadExecutor(daemonFactory);
        Constructor<ParallelTaskExecutor> ctor =
                ParallelTaskExecutor.class.getDeclaredConstructor(ExecutorService.class, int.class, ExecutorService.class);
        ctor.setAccessible(true);
        ParallelTaskExecutor executor = ctor.newInstance(platform, 1, null);
        try {
            AtomicInteger counter = new AtomicInteger();
            executor.scheduleVirtualThread(counter::incrementAndGet); // falls back to platform
            executor.awaitCompletion();
            assertEquals(1, counter.get());
            assertFalse(executor.supportsVirtualThreads());
            executor.shutdown();
            assertTrue(executor.awaitTermination(100, TimeUnit.MILLISECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("awaitTermination should handle virtual executor with zero timeout")
    void shouldAwaitTerminationWithVirtualExecutorZeroTimeout() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor();
        executor.shutdown();
        assertTrue(executor.awaitTermination(0, TimeUnit.NANOSECONDS));
    }

    @Test
    @DisplayName("awaitTermination should return false when platform tasks are still running")
    void shouldReturnFalseWhenPlatformTasksRunning() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(1);
        CountDownLatch block = new CountDownLatch(1);
        executor.schedulePlatformThread(() -> awaitSilently(block));
        executor.shutdown();
        assertFalse(executor.awaitTermination(1, TimeUnit.MILLISECONDS));
        block.countDown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("awaitTermination should return false when virtual tasks are still running")
    void shouldReturnFalseWhenVirtualTasksRunning() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor();
        CountDownLatch block = new CountDownLatch(1);
        executor.scheduleVirtualThread(() -> awaitSilently(block));
        executor.shutdown();
        assertFalse(executor.awaitTermination(1, TimeUnit.MILLISECONDS));
        block.countDown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("isTerminated should report false when virtual tasks are still running")
    void isTerminatedShouldBeFalseWhenVirtualRunning() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor();
        CountDownLatch block = new CountDownLatch(1);
        executor.scheduleVirtualThread(() -> awaitSilently(block));
        executor.shutdown();
        assertFalse(executor.isTerminated());
        block.countDown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("isTerminated should be false when both executors are still running tasks")
    void isTerminatedShouldBeFalseWhenBothExecutorsRunning() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(1);
        CountDownLatch platformBlock = new CountDownLatch(1);
        CountDownLatch virtualBlock = new CountDownLatch(1);
        executor.schedulePlatformThread(() -> awaitSilently(platformBlock));
        executor.scheduleVirtualThread(() -> awaitSilently(virtualBlock));
        executor.shutdown();
        assertFalse(executor.isTerminated());
        platformBlock.countDown();
        virtualBlock.countDown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("isTerminated should consider absence of virtual executor")
    void isTerminatedShouldHandleNullVirtualExecutor() throws Exception {
        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };
        ExecutorService platform = Executors.newSingleThreadExecutor(daemonFactory);
        Constructor<ParallelTaskExecutor> ctor =
                ParallelTaskExecutor.class.getDeclaredConstructor(ExecutorService.class, int.class, ExecutorService.class);
        ctor.setAccessible(true);
        ParallelTaskExecutor executor = ctor.newInstance(platform, 1, null);
        executor.shutdown();
        assertTrue(executor.isTerminated());
    }

    @Test
    @DisplayName("awaitTermination should short-circuit when virtual executor is absent")
    void shouldAwaitTerminationWithoutVirtualExecutor() throws Exception {
        ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };
        ExecutorService platform = Executors.newSingleThreadExecutor(daemonFactory);
        Constructor<ParallelTaskExecutor> ctor =
                ParallelTaskExecutor.class.getDeclaredConstructor(ExecutorService.class, int.class, ExecutorService.class);
        ctor.setAccessible(true);
        ParallelTaskExecutor executor = ctor.newInstance(platform, 1, null);
        executor.shutdown();
        assertTrue(executor.awaitTermination(100, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Should handle multiple awaitCompletion calls")
    void shouldHandleMultipleAwaitCompletionCalls() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);

        // When - first batch
        for (int i = 0; i < 5; i++) {
            executor.submit(counter::incrementAndGet);
        }
        executor.awaitCompletion();
        assertEquals(5, counter.get());

        // When - second batch
        for (int i = 0; i < 5; i++) {
            executor.submit(counter::incrementAndGet);
        }
        executor.awaitCompletion();

        // Then
        assertEquals(10, counter.get());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should create executor with default processor count")
    void shouldCreateExecutorWithDefaultProcessorCount() {
        // When
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor();

        // Then - should not throw and should be usable
        assertNotNull(executor);
        assertFalse(executor.isShutdown());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should return a reusable singleton instance")
    void shouldReturnReusableSingleton() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor1 = ParallelTaskExecutor.getInstance();
        ParallelTaskExecutor executor2 = ParallelTaskExecutor.getInstance();

        // Then - same instance should be returned
        assertSame(executor1, executor2);

        // And the singleton should accept work
        AtomicInteger counter = new AtomicInteger(0);
        executor1.submit(counter::incrementAndGet);
        executor1.awaitCompletion();
        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("Should handle rapid submit and await cycles")
    void shouldHandleRapidSubmitAndAwaitCycles() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);

        // When - perform multiple rapid cycles
        for (int cycle = 0; cycle < 10; cycle++) {
            for (int i = 0; i < 10; i++) {
                executor.submit(counter::incrementAndGet);
            }
            executor.awaitCompletion();
        }

        // Then
        assertEquals(100, counter.get());

        executor.shutdown();
    }

    private void awaitSilently(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ============================================
    // STRESS TESTS FOR CONCURRENCY ISSUES
    // ============================================

    @Test
    @DisplayName("Stress test: Lost wakeup scenario with rapid completion")
    void stressTestLostWakeupScenario() throws InterruptedException {
        // This test specifically targets the lost wakeup bug that was in awaitCompletion()
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(10);
        AtomicInteger failures = new AtomicInteger(0);
        int iterations = 100;

        try {
            for (int i = 0; i < iterations; i++) {
                CountDownLatch taskStarted = new CountDownLatch(1);
                CountDownLatch taskCanFinish = new CountDownLatch(1);

                // Submit a task that signals when started and waits
                executor.submit(() -> {
                    taskStarted.countDown();
                    awaitSilently(taskCanFinish);
                });

                // Wait for task to start
                assertTrue(taskStarted.await(1, TimeUnit.SECONDS));

                // Create a thread that will call awaitCompletion
                Thread waiter = new Thread(() -> {
                    try {
                        // This should complete after the task finishes
                        boolean completed = executor.awaitCompletion(2, TimeUnit.SECONDS);
                        if (!completed) {
                            failures.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failures.incrementAndGet();
                    }
                });

                waiter.start();

                // Small delay to ensure waiter thread is in awaitCompletion
                Thread.sleep(50);

                // Now complete the task
                taskCanFinish.countDown();

                // Waiter should complete
                waiter.join(3000);
                if (waiter.isAlive()) {
                    failures.incrementAndGet();
                    waiter.interrupt();
                }
            }

            assertEquals(0, failures.get(), "Lost wakeup detected in " + failures.get() + " iterations");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Concurrent shutdown and submission")
    void stressTestShutdownRaceCondition() throws InterruptedException {
        int iterations = 50;  // Reduced for faster test
        AtomicInteger successfulShutdowns = new AtomicInteger(0);
        AtomicInteger rejectedAfterShutdown = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(4);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch submittersDone = new CountDownLatch(5);
            AtomicInteger tasksExecuted = new AtomicInteger(0);
            AtomicBoolean shutdownCalled = new AtomicBoolean(false);

            // Create 5 submitter threads
            for (int j = 0; j < 5; j++) {
                Thread submitter = new Thread(() -> {
                    awaitSilently(startGate);
                    for (int k = 0; k < 20; k++) {
                        try {
                            executor.submit(tasksExecuted::incrementAndGet);
                        } catch (IllegalStateException | java.util.concurrent.RejectedExecutionException e) {
                            // Expected if shutdown happened
                            rejectedAfterShutdown.incrementAndGet();
                        }
                    }
                    submittersDone.countDown();
                });
                submitter.start();
            }

            // Single shutdown thread
            Thread shutdowner = new Thread(() -> {
                awaitSilently(startGate);
                executor.shutdown();
                shutdownCalled.set(true);
            });
            shutdowner.start();

            // Start all threads simultaneously
            startGate.countDown();

            // Wait for submitters to complete
            assertTrue(submittersDone.await(3, TimeUnit.SECONDS), "Submitters timed out");
            shutdowner.join(1000);

            // Wait for remaining tasks
            executor.awaitTermination(2, TimeUnit.SECONDS);

            if (executor.isShutdown()) {
                successfulShutdowns.incrementAndGet();
            }
        }

        // All iterations should have successful shutdowns
        assertEquals(iterations, successfulShutdowns.get());
        // Should have some rejections (proves shutdown blocks new submissions)
        assertTrue(rejectedAfterShutdown.get() > 0, "Should have rejected some tasks after shutdown");
    }

    @Test
    @DisplayName("Stress test: High concurrency with 1000+ threads")
    void stressTestHighConcurrency() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(16);
        int submitterThreads = 100;
        int tasksPerThread = 100;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch submittersDone = new CountDownLatch(submitterThreads);

        try {
            // Create many submitter threads
            List<Thread> submitters = new ArrayList<>();
            for (int i = 0; i < submitterThreads; i++) {
                Thread submitter = new Thread(() -> {
                    awaitSilently(startGate);
                    for (int j = 0; j < tasksPerThread; j++) {
                        executor.submit(counter::incrementAndGet);
                    }
                    submittersDone.countDown();
                });
                submitters.add(submitter);
                submitter.start();
            }

            // Start all threads
            startGate.countDown();

            // Wait for all submissions
            assertTrue(submittersDone.await(10, TimeUnit.SECONDS));

            // Wait for all tasks to complete
            executor.awaitCompletion();

            // Verify all tasks executed
            assertEquals(submitterThreads * tasksPerThread, counter.get());
            assertEquals(0, executor.getPendingTaskCount());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Concurrent awaitCompletion calls")
    void stressTestConcurrentAwaitCompletion() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(8);
        int waitersCount = 50;
        int tasksCount = 100;
        CountDownLatch tasksSubmitted = new CountDownLatch(1);
        CountDownLatch waitersComplete = new CountDownLatch(waitersCount);
        AtomicInteger failures = new AtomicInteger(0);
        AtomicInteger counter = new AtomicInteger(0);

        try {
            // Submit tasks first
            for (int i = 0; i < tasksCount; i++) {
                executor.submit(() -> {
                    counter.incrementAndGet();
                    // Small delay to ensure contention
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            tasksSubmitted.countDown();

            // Create many threads waiting for completion
            List<Thread> waiters = new ArrayList<>();
            for (int i = 0; i < waitersCount; i++) {
                Thread waiter = new Thread(() -> {
                    awaitSilently(tasksSubmitted);
                    try {
                        boolean completed = executor.awaitCompletion(10, TimeUnit.SECONDS);
                        if (!completed) {
                            failures.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failures.incrementAndGet();
                    } finally {
                        waitersComplete.countDown();
                    }
                });
                waiters.add(waiter);
                waiter.start();
            }

            // All waiters should complete
            assertTrue(waitersComplete.await(15, TimeUnit.SECONDS));

            // Verify no failures
            assertEquals(0, failures.get(), "Some waiters failed or timed out");
            assertEquals(tasksCount, counter.get());
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Rapid submit and awaitCompletion cycles")
    void stressTestRapidCycles() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(8);
        int cycles = 1000;
        AtomicInteger counter = new AtomicInteger(0);

        try {
            for (int i = 0; i < cycles; i++) {
                // Submit a few tasks
                for (int j = 0; j < 5; j++) {
                    executor.submit(counter::incrementAndGet);
                }
                // Immediately wait
                executor.awaitCompletion();
            }

            // Verify all tasks executed
            assertEquals(cycles * 5, counter.get());
            assertEquals(0, executor.getPendingTaskCount());
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Task submission during awaitCompletion")
    void stressTestSubmissionDuringAwait() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch firstBatchStarted = new CountDownLatch(1);
        CountDownLatch firstBatchCanFinish = new CountDownLatch(1);
        AtomicInteger awaitCompletedCount = new AtomicInteger(0);

        try {
            // Submit first batch of blocking tasks
            for (int i = 0; i < 4; i++) {
                executor.submit(() -> {
                    firstBatchStarted.countDown();
                    awaitSilently(firstBatchCanFinish);
                    counter.incrementAndGet();
                });
            }

            // Wait for tasks to start
            assertTrue(firstBatchStarted.await(1, TimeUnit.SECONDS));

            // Thread that waits for completion
            Thread waiter = new Thread(() -> {
                try {
                    executor.awaitCompletion();
                    awaitCompletedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            waiter.start();

            // Give waiter time to enter wait
            Thread.sleep(100);

            // Submit more tasks while waiter is waiting
            for (int i = 0; i < 10; i++) {
                executor.submit(counter::incrementAndGet);
            }

            // Release first batch
            firstBatchCanFinish.countDown();

            // Waiter should complete after ALL tasks finish
            waiter.join(3000);
            assertFalse(waiter.isAlive(), "Waiter should have completed");

            // Verify all tasks executed
            assertEquals(14, counter.get());
            assertEquals(1, awaitCompletedCount.get());
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Exception handling under load")
    void stressTestExceptionHandlingUnderLoad() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(8);
        int totalTasks = 1000;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < totalTasks; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    if (taskId % 3 == 0) {
                        exceptionCount.incrementAndGet();
                        throw new RuntimeException("Intentional exception " + taskId);
                    }
                    successCount.incrementAndGet();
                });
            }

            executor.awaitCompletion();

            // Verify correct counts - 0,3,6,9... throw exceptions
            int expectedExceptions = (totalTasks + 2) / 3;  // Ceiling division for 0-indexed
            int expectedSuccess = totalTasks - expectedExceptions;
            assertEquals(expectedSuccess, successCount.get());
            assertEquals(expectedExceptions, exceptionCount.get());
            assertEquals(0, executor.getPendingTaskCount());
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Memory visibility of task results")
    void stressTestMemoryVisibility() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(16);
        int size = 1000;
        int[] sharedArray = new int[size];
        AtomicInteger writesDone = new AtomicInteger(0);

        try {
            // Submit tasks that write to array
            for (int i = 0; i < size; i++) {
                final int index = i;
                executor.submit(() -> {
                    sharedArray[index] = index * 2;
                    writesDone.incrementAndGet();
                });
            }

            // Wait for completion
            executor.awaitCompletion();

            // Verify all writes are visible
            assertEquals(size, writesDone.get());
            for (int i = 0; i < size; i++) {
                assertEquals(i * 2, sharedArray[i], "Index " + i + " not properly visible");
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Platform and virtual thread interleaving")
    void stressTestMixedThreadTypes() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(8);
        int tasksPerType = 500;
        AtomicInteger platformCounter = new AtomicInteger(0);
        AtomicInteger virtualCounter = new AtomicInteger(0);

        try {
            // Interleave platform and virtual thread submissions
            for (int i = 0; i < tasksPerType; i++) {
                executor.schedulePlatformThread(platformCounter::incrementAndGet);
                executor.scheduleVirtualThread(virtualCounter::incrementAndGet);
            }

            executor.awaitCompletion();

            assertEquals(tasksPerType, platformCounter.get());
            assertEquals(tasksPerType, virtualCounter.get());
            assertEquals(0, executor.getPendingTaskCount());
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Stress test: Verify no task count leaks under heavy load")
    void stressTestNoTaskCountLeaks() throws InterruptedException {
        ParallelTaskExecutor executor = ParallelTaskExecutor.createExecutor(8);
        int iterations = 100;

        try {
            for (int iter = 0; iter < iterations; iter++) {
                // Submit batch
                for (int i = 0; i < 50; i++) {
                    executor.submit(() -> {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }

                // Wait for completion
                executor.awaitCompletion();

                // Verify counts are correct
                assertEquals(0, executor.getPendingTaskCount(),
                    "Pending count leaked at iteration " + iter);
                assertEquals(0, executor.getActiveTaskCount(),
                    "Active count leaked at iteration " + iter);
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
