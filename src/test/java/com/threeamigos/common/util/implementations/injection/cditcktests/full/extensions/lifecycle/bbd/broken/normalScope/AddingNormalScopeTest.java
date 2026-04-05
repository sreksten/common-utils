package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.broken.normalScope;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AddingNormalScopeTest {

    @Test
    void testAddingScopeType() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), AddingNormalScopeTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(BeforeBeanDiscoveryObserver.class.getName());

        try {
            assertThrows(DeploymentException.class, syringe::setup);
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
