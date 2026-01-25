package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * MessageHandler implementation that writes log messages to a file.
 * Supports optional async dispatch with a background worker and shutdown hook.
 */
public class FileMessageHandler extends AbstractMessageHandler implements AutoCloseable {

    private final PrintWriter writer;
    private final boolean async;
    private final BlockingQueue<Runnable> queue;
    private final ExecutorService worker;
    private final Thread shutdownHook;
    private final Object writeLock = new Object();

    public FileMessageHandler(final String filename) {
        this(filename, false, 0, false);
    }

    public FileMessageHandler(final String filename, final boolean async, final int queueCapacity) {
        this(filename, async, queueCapacity, false);
    }

    public FileMessageHandler(final String filename, final boolean async, final int queueCapacity, final boolean registerShutdownHook) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        Path filePath = Paths.get(filename);
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(filePath) && Files.isDirectory(filePath)) {
                throw new IllegalArgumentException("Path points to a directory: " + filePath);
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            if (!Files.isWritable(filePath)) {
                throw new IllegalArgumentException("File is not writable: " + filePath);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to prepare log file: " + filePath, e);
        }
        try {
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toFile(), true)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to open log file for writing: " + filePath, e);
        }

        this.async = async;
        if (async) {
            this.queue = queueCapacity > 0 ? new LinkedBlockingQueue<>(queueCapacity) : new LinkedBlockingQueue<>();
            this.worker = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FileMessageHandler-async");
                t.setDaemon(true);
                return t;
            });
            this.worker.submit(this::drainLoop);
            if (registerShutdownHook) {
                this.shutdownHook = new Thread(this::close, "FileMessageHandler-shutdown");
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
        writeLine(format("INFO ", message));
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        writeLine(format("WARN ", message));
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        writeLine(format("ERROR", message));
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        writeLine(format("DEBUG", message));
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        writeLine(format("TRACE", message));
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        writeLine(format("EXCEP", exception.getMessage()));
        dispatch(() -> {
            synchronized (writeLock) {
                exception.printStackTrace(writer);
                writer.flush();
            }
        });
    }

    private String format(String level, String message) {
        String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return String.format("[%s] [%s] %s", date, level, message);
    }

    private void writeLine(String line) {
        dispatch(() -> {
            synchronized (writeLock) {
                writer.println(line);
                writer.flush();
            }
        });
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

    @Override
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
        synchronized (writeLock) {
            writer.flush();
            writer.close();
        }
    }
}
