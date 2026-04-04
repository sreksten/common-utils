package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.tooManyParameters;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverMethodWithTwoEventParametersTest {

    @Test
    void testObserverMethodMustHaveOnlyOneEventParameter() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), YorkshireTerrier_Broken.class, Boxer.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class YorkshireTerrier_Broken {
        void observesAfterBeanDiscovery(@Observes AfterBeanDiscovery beforeBeanDiscovery, @Observes Boxer anotherDog) {
        }
    }

    static class Boxer {
    }
}
