package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated.broken.processInjectionTargetThrowsException;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessInjectionTargetEventThrowsExceptionTest {

    @Test
    void testProcessInjectionTargetEventThrowsExceptionNotOk() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Dog.class,
                InjectionTargetProcessor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(InjectionTargetProcessor.class.getName());
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
