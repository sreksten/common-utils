package com.threeamigos.common.util.implementations.concurrency;

import jakarta.annotation.Nonnull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A generic parallel task executor that manages a pool of threads to execute tasks concurrently.
 *
 * <p>This executor provides the following capabilities:
 * <ul>
 *   <li>Submit tasks at any time - they are queued and executed when threads are available</li>
 *   <li>Configurable thread pool size with support for both platform and virtual threads</li>
 *   <li>Graceful shutdown and interruption of running tasks</li>
 *   <li>Ability to wait for all queued tasks to complete</li>
 *   <li>Thread-safe task submission and completion tracking</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // For I/O-bound tasks (file scanning, network calls)
 * ParallelTaskExecutor executor = ParallelTaskExecutor.withVirtualThreads();
 *
 * // Submit tasks
 * executor.submit(() -> processFile("file1.txt"));
 * executor.submit(() -> processFile("file2.txt"));
 *
 * // Wait for all tasks to complete
 * executor.awaitCompletion();
 *
 * // Shutdown when done
 * executor.shutdown();
 * }</pre>
 *
 * <p><b>Thread Type Selection:</b>
 * <ul>
 *   <li><b>Virtual Threads</b>: Best for I/O-bound tasks (file reading, network calls, database queries).
 *       Can handle millions of concurrent tasks with minimal overhead.</li>
 *   <li><b>Platform Threads</b>: Best for CPU-bound tasks (computation, parsing, transformation).
 *       Limited by available CPU cores.</li>
 * </ul>
 *
 * <p>This class is thread-safe and can be used concurrently from multiple threads.
 *
 * @author Stefano Reksten
 * @see ThreadPoolExecutor
 * @see ExecutorService
 */
public class ParallelTaskExecutor implements AutoCloseable {

    private final ExecutorService executorService;
    private final AtomicInteger pendingTasks;
    private final Object completionLock;
    private volatile boolean isShutdown;

    /**
     * Creates a new executor with the specified thread pool.
     *
     * @param executorService the executor service to use for running tasks
     * @throws IllegalArgumentException if executorService is null
     */
    private ParallelTaskExecutor(ExecutorService executorService) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        this.executorService = executorService;
        this.pendingTasks = new AtomicInteger(0);
        this.completionLock = new Object();
        this.isShutdown = false;
    }

    /**
     * Creates an executor optimized for I/O-bound tasks using virtual threads.
     * Virtual threads are lightweight and can handle millions of concurrent tasks.
     *
     * <p><b>Best for:</b> File I/O, network calls, database queries, classpath scanning
     *
     * <p><b>Requires:</b> Java 21 or later
     *
     * @return a new executor using virtual threads
     * @throws UnsupportedOperationException if virtual threads are not available (Java < 21)
     */
    public static ParallelTaskExecutor withVirtualThreads() {
        try {
            // Use reflection to avoid compile-time dependency on Java 21
            // Equivalent to: Executors.newVirtualThreadPerTaskExecutor()
            java.lang.reflect.Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            ExecutorService executor = (ExecutorService) method.invoke(null);
            return new ParallelTaskExecutor(executor);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(
                "Virtual threads require Java 21 or later. Use withPlatformThreads() instead.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create virtual thread executor", e);
        }
    }

    /**
     * Creates an executor for CPU-bound tasks using a fixed pool of platform threads.
     * The pool size is based on the number of available processors.
     *
     * <p><b>Best for:</b> CPU-intensive computation, parsing, data transformation
     *
     * @return a new executor using platform threads (size = available processors)
     */
    public static ParallelTaskExecutor withPlatformThreads() {
        return withPlatformThreads(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates an executor for CPU-bound tasks using a fixed pool of platform threads
     * with the specified pool size.
     *
     * <p><b>Best for:</b> CPU-intensive computation, parsing, data transformation
     *
     * @param threadPoolSize the number of threads in the pool
     * @return a new executor using platform threads
     * @throws IllegalArgumentException if threadPoolSize is less than 1
     */
    public static ParallelTaskExecutor withPlatformThreads(int threadPoolSize) {
        if (threadPoolSize < 1) {
            throw new IllegalArgumentException("Thread pool size must be at least 1");
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            threadPoolSize,                    // core pool size
            threadPoolSize,                    // maximum pool size
            60L,                               // keep-alive time
            TimeUnit.SECONDS,                  // time unit
            new LinkedBlockingQueue<>(),       // work queue (unbounded)
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(@Nonnull Runnable r) {
                    Thread t = new Thread(r, "ParallelTaskExecutor-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            }
        );

        return new ParallelTaskExecutor(executor);
    }

    /**
     * Submits a task for execution. The task will be queued and executed when a thread
     * becomes available. This method returns immediately.
     *
     * @param task the task to execute
     * @throws IllegalStateException if the executor has been shut down
     * @throws IllegalArgumentException if task is null
     */
    public void submit(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (isShutdown) {
            throw new IllegalStateException("Executor has been shut down");
        }

        pendingTasks.incrementAndGet();

        executorService.submit(() -> {
            try {
                task.run();
            } finally {
                int remaining = pendingTasks.decrementAndGet();
                if (remaining == 0) {
                    synchronized (completionLock) {
                        completionLock.notifyAll();
                    }
                }
            }
        });
    }

    /**
     * Blocks until all currently queued and running tasks have completed.
     * New tasks submitted after this method is called will also be waited for
     * until this method returns.
     *
     * <p>This method is useful when you want to submit a batch of tasks and
     * then wait for all of them to complete before continuing.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * executor.submit(() -> task1());
     * executor.submit(() -> task2());
     * executor.submit(() -> task3());
     * executor.awaitCompletion(); // Blocks until all 3 tasks complete
     * }</pre>
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void awaitCompletion() throws InterruptedException {
        synchronized (completionLock) {
            while (pendingTasks.get() > 0) {
                completionLock.wait();
            }
        }
    }

    /**
     * Blocks until all currently queued and running tasks have completed, or the timeout expires.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if all tasks completed, false if the timeout elapsed
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws IllegalArgumentException if timeout is negative or unit is null
     */
    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        if (unit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        long deadline = System.nanoTime() + unit.toNanos(timeout);

        synchronized (completionLock) {
            while (pendingTasks.get() > 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                completionLock.wait(TimeUnit.NANOSECONDS.toMillis(remaining));
            }
        }
        return true;
    }

    /**
     * Returns the number of tasks currently queued or running.
     *
     * @return the number of pending tasks
     */
    public int getPendingTaskCount() {
        return pendingTasks.get();
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed,
     * but no new tasks will be accepted. This method does not wait for tasks to complete.
     * Use {@link #awaitCompletion()} before shutdown to wait for all tasks to finish.
     *
     * <p>This method does not block. Use {@link #awaitTermination(long, TimeUnit)} to
     * wait for the executor to terminate.
     */
    public void shutdown() {
        isShutdown = true;
        executorService.shutdown();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the processing of waiting tasks,
     * and returns a list of the tasks that were awaiting execution.
     *
     * <p>This method does not wait for actively executing tasks to terminate.
     * Use {@link #awaitTermination(long, TimeUnit)} to wait for termination.
     *
     * <p><b>Warning:</b> There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks. For example, typical implementations will cancel
     * via {@link Thread#interrupt}, so any task that fails to respond to interrupts may
     * never terminate.
     */
    public void shutdownNow() {
        isShutdown = true;
        executorService.shutdownNow();
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if this executor terminated, false if the timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    /**
     * Returns true if this executor has been shut down.
     *
     * @return true if this executor has been shut down
     */
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Returns true if all tasks have completed following shutdown.
     *
     * @return true if all tasks have completed following shutdown
     */
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    /**
     * Initiates an orderly shutdown and waits for all tasks to complete.
     * This is a convenience method equivalent to calling {@link #shutdown()}
     * followed by {@link #awaitTermination(long, TimeUnit)} with a long timeout.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public void close() throws InterruptedException {
        shutdown();
        awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}
