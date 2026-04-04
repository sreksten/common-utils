package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.broken.validation.unsatisfied;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserverMethodParameterInjectionValidationTest {

    @Test
    void test() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Observer.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class Observer {
        void observe(@Observes String event, File file) {
        }
    }
}
