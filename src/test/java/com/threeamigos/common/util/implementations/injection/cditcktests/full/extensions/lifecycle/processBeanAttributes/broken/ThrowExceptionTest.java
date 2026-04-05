package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.broken;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowExceptionTest {

    @Test
    void testDeployment() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ThrowExceptionExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(BrokenException.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Duke.class, BeanArchiveMode.EXPLICIT);

        try {
            assertThrows(DefinitionException.class, syringe::start);
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
