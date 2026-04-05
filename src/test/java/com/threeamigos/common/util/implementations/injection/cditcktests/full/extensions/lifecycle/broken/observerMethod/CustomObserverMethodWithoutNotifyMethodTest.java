package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.observerMethod;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomObserverMethodWithoutNotifyMethodTest {

    @Test
    void observerCustomMethodNotOverridingNotifyMethodTreatedAsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), CustomObserverMethodWithoutNotifyMethodTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ExtensionAddingCustomObserverMethod.class.getName());

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
