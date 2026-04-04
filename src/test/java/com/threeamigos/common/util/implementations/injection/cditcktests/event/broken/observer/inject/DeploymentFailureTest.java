package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.inject;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DeploymentFailureTest {

    @Test
    void testDeploymentFailsBeforeNotifyingObserversAfterBeanDiscovery() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InitializerBean_Broken.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class InitializerBean_Broken {
        @Inject
        void initialize(@Observes String string) {
        }
    }
}
