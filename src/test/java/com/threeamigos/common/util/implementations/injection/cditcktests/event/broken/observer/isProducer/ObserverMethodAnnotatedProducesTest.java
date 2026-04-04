package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.isProducer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverMethodAnnotatedProducesTest {

    @Test
    void testObserverMethodAnnotatedProducesFails() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), BorderTerrier_Broken.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class BorderTerrier_Broken {
        @Produces
        String observesAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
            return "product";
        }
    }
}
