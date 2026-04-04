package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.isDisposer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverMethodAnnotatedDisposesTest {

    @Test
    void testObserverMethodWithDisposesParamFails() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), FoxTerrier_Broken.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class FoxTerrier_Broken {
        void observeInitialized(@Observes BeforeBeanDiscovery event, @Disposes String badParam) {
        }
    }
}
