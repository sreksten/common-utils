package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of the {@link MessageHandler} interface that uses the
 * console to print info, warning, trace, and debug messages to System.out and errors and
 * exceptions to System.err.
 *
 * @author Stefano Reksten
 */
public class ConsoleMessageHandler extends AbstractMessageHandler implements AutoCloseable {

    private static final Object PRINT_LOCK = new Object();
    private final boolean async;
    private final BlockingQueue<Runnable> queue;
    private final ExecutorService worker;
    private final Thread shutdownHook;

    public ConsoleMessageHandler() {
        this(false, 0, false);
    }

    /**
     * @param async whether to dispatch logging to a background worker
     * @param queueCapacity capacity for the async queue; 0 or negative => unbounded
     */
    public ConsoleMessageHandler(boolean async, int queueCapacity) {
        this(async, queueCapacity, false);
    }

    /**
     * @param async whether to dispatch logging to a background worker
     * @param queueCapacity capacity for the async queue; 0 or negative => unbounded
     * @param registerShutdownHook whether to register a JVM shutdown hook to close the handler
     */
    public ConsoleMessageHandler(boolean async, int queueCapacity, boolean registerShutdownHook) {
        this.async = async;
        if (async) {
            this.queue = queueCapacity > 0 ? new LinkedBlockingQueue<>(queueCapacity) : new LinkedBlockingQueue<>();
            this.worker = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ConsoleMessageHandler-async");
                t.setDaemon(true);
                return t;
            });
            this.worker.submit(this::drainLoop);
            if (registerShutdownHook) {
                this.shutdownHook = new Thread(this::close, "ConsoleMessageHandler-shutdown");
                Runtime.getRuntime().addShutdownHook(this.shutdownHook);
            } else {
                this.shutdownHook = null;
            }
        } else {
            this.queue = null;
            this.worker = null;
            this.shutdownHook = null;
        }
    }

    @Override
    protected void handleInfoMessageImpl(final String message) {
        print(System.out, format("INFO ", message));
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        print(System.out, format("WARN ", message));
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        print(System.err, format("ERROR", message));
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        print(System.out, format("DEBUG", message));
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        print(System.out, format("TRACE", message));
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        Runnable task = () -> {
            synchronized (PRINT_LOCK) {
                System.err.println(format("EXCEP", exception.getMessage()));
                exception.printStackTrace(System.err); //NOSONAR
            }
        };
        dispatch(task);
    }

    @Override
    protected void handleExceptionImpl(final String message, final Exception exception) {
        Runnable task = () -> {
            synchronized (PRINT_LOCK) {
                System.err.println(format("EXCEP", message));
                System.err.println(format("EXCEP", exception.getMessage()));
                exception.printStackTrace(System.err); //NOSONAR
            }
        };
        dispatch(task);
    }

    private String format(String level, String message) {
        String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return String.format("[%s] [%s] %s", date, level, message);
    }

    private void print(PrintStream stream, String formatted) {
        Runnable task = () -> {
            synchronized (PRINT_LOCK) {
                stream.println(formatted);
            }
        };
        dispatch(task);
    }

    private void dispatch(Runnable task) {
        if (!async) {
            task.run();
            return;
        }
        if (!queue.offer(task)) {
            // queue full: run synchronously to avoid losing messages
            task.run();
        }
    }

    private void drainLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Runnable task = queue.take();
                task.run();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            Runnable task;
            while ((task = queue.poll()) != null) {
                task.run();
            }
        }
    }

    public void close() {
        if (worker != null) {
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ignored) {
                    // JVM is shutting down
                }
            }
            worker.shutdownNow();
            try {
                worker.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
