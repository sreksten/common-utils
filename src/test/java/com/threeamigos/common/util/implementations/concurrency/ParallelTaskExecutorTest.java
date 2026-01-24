package com.threeamigos.common.util.implementations.concurrency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParallelTaskExecutor Tests")
class ParallelTaskExecutorTest {

    @Test
    @DisplayName("Should execute tasks in parallel with platform threads")
    void shouldExecuteTasksInParallelWithPlatformThreads() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(4);
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

    @Test
    @DisplayName("Should execute tasks with virtual threads when available")
    void shouldExecuteTasksWithVirtualThreads() throws InterruptedException {
        // Given - skip test if virtual threads not available (Java < 21)
        ParallelTaskExecutor executor;
        try {
            executor = ParallelTaskExecutor.withVirtualThreads();
        } catch (UnsupportedOperationException e) {
            // Java < 21, skip test
            return;
        }

        try {
            AtomicInteger counter = new AtomicInteger(0);
            int taskCount = 1000; // Virtual threads can handle many more

            // When
            for (int i = 0; i < taskCount; i++) {
                executor.submit(counter::incrementAndGet);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(1);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);
        AtomicInteger counter = new AtomicInteger(0);

        // When
        for (int i = 0; i < 10; i++) {
            executor.submit(counter::incrementAndGet);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);
        executor.shutdown();

        // When/Then
        assertThrows(IllegalStateException.class, () -> executor.submit(() -> {}));
    }

    @Test
    @DisplayName("Should handle exceptions in tasks gracefully")
    void shouldHandleExceptionsInTasksGracefully() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - submit mix of successful and failing tasks
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
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
        try (ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2)) {
            for (int i = 0; i < 10; i++) {
                executor.submit(counter::incrementAndGet);
            }
            executor.awaitCompletion();
        }

        // Then - executor should be closed automatically
        assertEquals(10, counter.get());
    }

    @Test
    @DisplayName("Should handle concurrent submissions from multiple threads")
    void shouldHandleConcurrentSubmissionsFromMultipleThreads() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(4);
        AtomicInteger counter = new AtomicInteger(0);
        int submitterThreads = 5;
        int tasksPerThread = 20;
        CountDownLatch submissionLatch = new CountDownLatch(submitterThreads);

        // When - submit tasks from multiple threads concurrently
        List<Thread> submitters = new ArrayList<>();
        for (int i = 0; i < submitterThreads; i++) {
            Thread submitter = new Thread(() -> {
                for (int j = 0; j < tasksPerThread; j++) {
                    executor.submit(counter::incrementAndGet);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(4);
        ConcurrentHashMap<Long, Boolean> threadIds = new ConcurrentHashMap<>();

        // When
        for (int i = 0; i < 20; i++) {
            executor.submit(() -> threadIds.put(Thread.currentThread().getId(), true));
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> executor.submit(null));

        executor.shutdown();
    }

    @Test
    @DisplayName("Should throw exception for invalid thread pool size")
    void shouldThrowExceptionForInvalidThreadPoolSize() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> ParallelTaskExecutor.withPlatformThreads(0));
        assertThrows(IllegalArgumentException.class, () -> ParallelTaskExecutor.withPlatformThreads(-1));
    }

    @Test
    @DisplayName("Should throw exception for invalid timeout")
    void shouldThrowExceptionForInvalidTimeout() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);

        // When/Then
        assertThrows(IllegalArgumentException.class,
            () -> executor.awaitCompletion(-1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
            () -> executor.awaitCompletion(1, null));

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle awaitCompletion when no tasks submitted")
    void shouldHandleAwaitCompletionWhenNoTasksSubmitted() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);

        // When
        executor.awaitCompletion(); // Should return immediately

        // Then
        assertEquals(0, executor.getPendingTaskCount());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle multiple awaitCompletion calls")
    void shouldHandleMultipleAwaitCompletionCalls() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(2);
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
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads();

        // Then - should not throw and should be usable
        assertNotNull(executor);
        assertFalse(executor.isShutdown());

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle rapid submit and await cycles")
    void shouldHandleRapidSubmitAndAwaitCycles() throws InterruptedException {
        // Given
        ParallelTaskExecutor executor = ParallelTaskExecutor.withPlatformThreads(4);
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
}
