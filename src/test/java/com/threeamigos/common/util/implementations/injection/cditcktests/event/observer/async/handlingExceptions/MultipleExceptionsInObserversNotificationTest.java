package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.async.handlingExceptions;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.ObservesAsync;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class MultipleExceptionsInObserversNotificationTest {

    @Test
    void testMultipleExceptionsDuringVariousObserversNotification() throws Exception {
        Syringe syringe = newSyringe();
        try {
            cleanup();
            BlockingQueue<Throwable> queue = new LinkedBlockingQueue<Throwable>();
            syringe.getBeanManager().getEvent()
                    .select(RadioMessage.class)
                    .fireAsync(new RadioMessage("ping"))
                    .handle((event, throwable) -> queue.offer(throwable));

            Throwable throwable = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(throwable);
            assertTrue(NewYorkRadioStation.observed.get());
            assertTrue(ParisRadioStation.observed.get());
            assertTrue(PragueRadioStation.observed.get());

            assertTrue(throwable instanceof CompletionException);

            List<Throwable> suppressedExceptions = Arrays.asList(throwable.getSuppressed());
            assertEquals(2, suppressedExceptions.size());
            assertTrue(suppressedExceptions.contains(ParisRadioStation.exception.get()));
            assertTrue(suppressedExceptions.contains(NewYorkRadioStation.exception.get()));
            assertTrue(suppressedExceptions.stream().anyMatch(t -> t.getMessage().equals(ParisRadioStation.class.getName())));
            assertTrue(suppressedExceptions.stream().anyMatch(t -> t.getMessage().equals(NewYorkRadioStation.class.getName())));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSingleExceptionDuringVariousObserversNotification() throws Exception {
        Syringe syringe = newSyringe();
        try {
            cleanup();
            BlockingQueue<Throwable> queue = new LinkedBlockingQueue<Throwable>();
            syringe.getBeanManager().getEvent()
                    .select(RadioMessage.class)
                    .fireAsync(new RadioMessage("pong"))
                    .handle((event, throwable) -> queue.offer(throwable));

            Throwable throwable = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(throwable);
            assertTrue(NewYorkRadioStation.observed.get());
            assertTrue(ParisRadioStation.observed.get());
            assertTrue(PragueRadioStation.observed.get());

            assertTrue(throwable instanceof CompletionException);

            List<Throwable> suppressedExceptions = Arrays.asList(throwable.getSuppressed());
            assertEquals(1, suppressedExceptions.size());
            assertTrue(suppressedExceptions.contains(ParisRadioStation.exception.get()));
            assertTrue(suppressedExceptions.stream().anyMatch(t -> t.getMessage().equals(ParisRadioStation.class.getName())));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                RadioMessage.class,
                NewYorkRadioStation.class,
                ParisRadioStation.class,
                PragueRadioStation.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private void cleanup() {
        NewYorkRadioStation.exception = null;
        ParisRadioStation.exception = null;
        NewYorkRadioStation.observed.set(false);
        ParisRadioStation.observed.set(false);
        PragueRadioStation.observed.set(false);
    }

    static class RadioMessage {
        private final String message;

        RadioMessage(String message) {
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

    @Dependent
    static class NewYorkRadioStation {
        static AtomicBoolean observed = new AtomicBoolean(false);
        static AtomicReference<Exception> exception;

        void observe(@ObservesAsync RadioMessage radioMessage) throws Exception {
            observed.set(true);
            if ("ping".equals(radioMessage.getMessage())) {
                exception = new AtomicReference<Exception>(new IllegalStateException(NewYorkRadioStation.class.getName()));
                throw exception.get();
            }
        }
    }

    @Dependent
    static class ParisRadioStation {
        static AtomicBoolean observed = new AtomicBoolean(false);
        static AtomicReference<Exception> exception;

        void observe(@ObservesAsync RadioMessage radioMessage) throws Exception {
            observed.set(true);
            exception = new AtomicReference<Exception>(new RuntimeException(ParisRadioStation.class.getName()));
            throw exception.get();
        }
    }

    @Dependent
    static class PragueRadioStation {
        static AtomicBoolean observed = new AtomicBoolean(false);

        void observe(@ObservesAsync RadioMessage radioMessage) {
            observed.set(true);
        }
    }
}
