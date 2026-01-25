package com.threeamigos.common.util.implementations.messagehandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileMessageHandler unit tests")
class FileMessageHandlerUnitTest {

    @Test
    @DisplayName("Should reject null or empty path")
    void shouldRejectNullOrEmptyPath() {
        assertThrows(IllegalArgumentException.class, () -> new FileMessageHandler(null));
        assertThrows(IllegalArgumentException.class, () -> new FileMessageHandler(" "));
    }

    @Test
    @DisplayName("Should reject directory path")
    void shouldRejectDirectoryPath() throws IOException {
        Path dir = Files.createTempDirectory("fmh-dir");
        assertThrows(IllegalArgumentException.class, () -> new FileMessageHandler(dir.toString()));
    }

    @Test
    @DisplayName("Should reject non-writable file")
    void shouldRejectNonWritableFile() throws IOException {
        Path file = Files.createTempFile("fmh-ro", ".log");
        file.toFile().setWritable(false);
        try {
            assertThrows(IllegalArgumentException.class, () -> new FileMessageHandler(file.toString()));
        } finally {
            file.toFile().setWritable(true);
        }
    }

    @Test
    @DisplayName("Should write messages to file and close cleanly")
    void shouldWriteMessagesToFile() throws Exception {
        Path file = Files.createTempFile("fmh", ".log");
        Files.deleteIfExists(file);
        try (FileMessageHandler handler = new FileMessageHandler(file.toString())) {
            handler.handleInfoMessage("info");
            handler.handleWarnMessage("warn");
            handler.handleErrorMessage("error");
            handler.handleDebugMessage("debug");
            handler.handleTraceMessage("trace");
            handler.handleException(new RuntimeException("boom"));
        }
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.stream().anyMatch(l -> l.contains("INFO ") && l.endsWith("info")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("WARN ") && l.endsWith("warn")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("ERROR") && l.endsWith("error")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("DEBUG") && l.endsWith("debug")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("TRACE") && l.endsWith("trace")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("EXCEP") && l.contains("boom")));
    }

    @Test
    @DisplayName("Async with shutdown hook should register hook and close cleanly")
    void asyncWithShutdownHookRegistersHook() throws Exception {
        Path file = Files.createTempFile("fmh", ".log");
        Files.deleteIfExists(file);
        FileMessageHandler handler = new FileMessageHandler(file.toString(), true, 10, true);
        try {
            handler.handleInfoMessage("hello");
        } finally {
            handler.close();
        }
    }

    @Test
    @DisplayName("Async with full queue should fallback to synchronous execution")
    void asyncQueueFullFallsBackToSync() throws Exception {
        Path file = Files.createTempFile("fmh", ".log");
        Files.deleteIfExists(file);
        try (FileMessageHandler handler = new FileMessageHandler(file.toString(), true, 1)) {
            handler.handleInfoMessage(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                return "background";
            });
            handler.handleInfoMessage("sync");
            Thread.sleep(300); // allow background to finish
        }
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.stream().anyMatch(l -> l.contains("sync")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("background")));
    }

    @Test
    @DisplayName("Async with non-positive capacity should behave as unbounded")
    void asyncWithNonPositiveCapacityBehavesUnbounded() throws Exception {
        Path file = Files.createTempFile("fmh", ".log");
        Files.deleteIfExists(file);
        try (FileMessageHandler handler = new FileMessageHandler(file.toString(), true, 0)) {
            handler.handleInfoMessage("msg1");
            handler.handleInfoMessage("msg2");
            Thread.sleep(100);
        }
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.stream().anyMatch(l -> l.contains("msg1")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("msg2")));
    }

    @Test
    @DisplayName("Overflow should execute synchronously (dispatch fallback)")
    void overflowShouldExecuteSynchronously() throws Exception {
        Path file = Files.createTempFile("fmh", ".log");
        Files.deleteIfExists(file);
        try (FileMessageHandler handler = new FileMessageHandler(file.toString(), true, 1)) {
            // Fill the queue with a blocking task
            handler.handleInfoMessage(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                return "blocking";
            });
            // This should run synchronously due to full queue
            handler.handleInfoMessage("sync-fallback");
            Thread.sleep(250);
        }
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.stream().anyMatch(l -> l.contains("sync-fallback")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("blocking")));
    }

    @Test
    @DisplayName("Drain loop should handle high volume without losing messages")
    void drainLoopHighVolume() throws Exception {
        Path file = Files.createTempFile("fmh", ".log");
        Files.deleteIfExists(file);
        int count = 200;
        try (FileMessageHandler handler = new FileMessageHandler(file.toString(), true, 50)) {
            for (int i = 0; i < count; i++) {
                int idx = i;
                handler.handleInfoMessage(() -> "msg-" + idx);
            }
            Thread.sleep(500);
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(count, lines.stream().filter(l -> l.contains("INFO ")).count());
    }

    @Test
    @DisplayName("Should be safe to close multiple times")
    void shouldAllowIdempotentClose() throws Exception {
        Path file = Files.createTempFile("fmh", ".log");
        Files.deleteIfExists(file);
        FileMessageHandler handler = new FileMessageHandler(file.toString());
        handler.handleInfoMessage("once");
        handler.close();
        handler.close(); // should not throw
        List<String> lines = Files.readAllLines(file);
        assertTrue(lines.stream().anyMatch(l -> l.contains("once")));
    }

    @Test
    @DisplayName("Should reject when parent directory is not writable")
    void shouldRejectNonWritableDirectory() throws Exception {
        Path dir = Files.createTempDirectory("fmh-nw");
        dir.toFile().setWritable(false);
        Path file = dir.resolve("log.log");
        try {
            assertThrows(IllegalArgumentException.class, () -> new FileMessageHandler(file.toString()));
        } finally {
            dir.toFile().setWritable(true);
        }
    }
}
