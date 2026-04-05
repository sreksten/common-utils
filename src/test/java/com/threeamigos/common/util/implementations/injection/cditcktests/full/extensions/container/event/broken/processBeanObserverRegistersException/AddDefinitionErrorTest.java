package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.container.event.broken.processBeanObserverRegistersException;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AddDefinitionErrorTest {

    @Test
    void testAddDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ProcessBeanObserver.class, Sheep.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessBeanObserver.class.getName());

        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
