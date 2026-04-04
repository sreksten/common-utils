package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.tooManyParameters;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverMethodWithObservesAndObservesAsyncParametersTest {

    @Test
    void testObserverMethodMustHaveOnlyOneEventParameter() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Poodle_Broken.class, Boxer.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class Poodle_Broken {
        void observes(@Observes AfterDeploymentValidation beforeBeanDiscovery, @ObservesAsync Boxer anotherDog) {
        }
    }

    static class Boxer {
    }
}
