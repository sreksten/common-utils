package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated.delivery.broken;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WithAnnotationsAppliedToIllegalEventParameterTest {

    @Test
    void testDeploymentFails() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ApplicationObserver.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
