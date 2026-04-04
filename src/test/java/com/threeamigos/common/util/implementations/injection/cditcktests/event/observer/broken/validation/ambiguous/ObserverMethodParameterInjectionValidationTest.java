package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.broken.validation.ambiguous;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverMethodParameterInjectionValidationTest {

    @Test
    void test() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Observer.class,
                Animal.class,
                Cow.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    static class Animal {
    }

    static class Cow extends Animal {
    }

    @Dependent
    static class Observer {
        void observe(@Observes String event, Animal animal) {
        }
    }
}
