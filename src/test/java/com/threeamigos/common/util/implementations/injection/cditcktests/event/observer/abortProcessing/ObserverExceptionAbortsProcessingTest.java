package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.abortProcessing;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverExceptionAbortsProcessingTest {

    @Test
    void testObserverThrowsExceptionAbortsNotifications() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                AnEventType.class, AnObserverWithException.class, AnotherObserverWithException.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            AnObserverWithException.wasNotified = false;
            AnotherObserverWithException.wasNotified = false;

            boolean fireFailed = false;
            try {
                syringe.getBeanManager().getEvent().select(AnEventType.class).fire(new AnEventType());
            } catch (Exception e) {
                if (e.equals(AnObserverWithException.theException)) {
                    fireFailed = true;
                    assertTrue(AnObserverWithException.wasNotified);
                    assertFalse(AnotherObserverWithException.wasNotified);
                } else if (e.equals(AnotherObserverWithException.theException)) {
                    fireFailed = true;
                    assertFalse(AnObserverWithException.wasNotified);
                    assertTrue(AnotherObserverWithException.wasNotified);
                }
            }
            assertTrue(fireFailed);
        } finally {
            syringe.shutdown();
        }
    }

    static class AnEventType {
    }

    @Dependent
    static class AnObserverWithException {
        static boolean wasNotified;
        static final RuntimeException theException = new RuntimeException("RE1");

        void observer(@Observes AnEventType event) {
            wasNotified = true;
            throw theException;
        }
    }

    @Dependent
    static class AnotherObserverWithException {
        static boolean wasNotified;
        static final RuntimeException theException = new RuntimeException("RE2");

        void observer(@Observes AnEventType event) {
            wasNotified = true;
            throw theException;
        }
    }
}
