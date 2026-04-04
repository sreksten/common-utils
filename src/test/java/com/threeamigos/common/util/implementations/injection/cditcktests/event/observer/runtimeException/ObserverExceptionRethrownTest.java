package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.runtimeException;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverExceptionRethrownTest {

    @Test
    void testNonTransactionalObserverThrowsNonCheckedExceptionIsRethrown() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(TeaCupPomeranian.OversizedException.class,
                    () -> syringe.getBeanManager().getEvent().select(TeaCupPomeranian.Trigger.class)
                            .fire(new TeaCupPomeranian.Trigger()));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), TeaCupPomeranian.class, TeaCupPomeranian.Trigger.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @Dependent
    static class TeaCupPomeranian {

        static class OversizedException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

        static class Trigger {
        }

        public void observeSimpleEvent(@Observes Trigger someEvent) {
            throw new OversizedException();
        }
    }
}
