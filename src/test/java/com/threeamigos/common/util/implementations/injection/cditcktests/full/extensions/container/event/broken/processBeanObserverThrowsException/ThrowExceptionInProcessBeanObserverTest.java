package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.container.event.broken.processBeanObserverThrowsException;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowExceptionInProcessBeanObserverTest {

    @Test
    void testProcessBeanObserverThrowsException() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Sheep.class, ProcessBeanObserver.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessBeanObserver.class.getName());
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
