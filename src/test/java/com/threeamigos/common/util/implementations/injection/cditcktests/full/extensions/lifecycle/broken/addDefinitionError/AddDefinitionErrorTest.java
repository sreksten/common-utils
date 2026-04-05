package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.addDefinitionError;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AddDefinitionErrorTest {

    @Test
    void testObserverDefinitionErrorTreatedAsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), AddDefinitionErrorTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());

        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            safeShutdown(syringe);
        }
    }

    private static void safeShutdown(Syringe syringe) {
        try {
            syringe.shutdown();
        } catch (Exception ignored) {
            // Startup failed as expected.
        }
    }
}
