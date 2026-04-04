package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.bothObservesAnnotations;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverMethodParameterAnnotatedAsSyncAndAsyncTest {

    @Test
    void observerMethodMustBeSyncOrAsync() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), BrokenObserver.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class BrokenObserver {
        void observe(@Observes @ObservesAsync Integer test) {
        }
    }
}
