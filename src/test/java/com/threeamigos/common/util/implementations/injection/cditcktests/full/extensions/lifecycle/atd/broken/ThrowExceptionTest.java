package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.broken;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowExceptionTest {

    @Test
    void testDeployment() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ThrowExceptionTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ThrowExceptionExtension.class.getName());

        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            safeShutdown(syringe);
        }
    }

    private void safeShutdown(Syringe syringe) {
        try {
            syringe.shutdown();
        } catch (Exception ignored) {
            // Startup failed as expected.
        }
    }
}
