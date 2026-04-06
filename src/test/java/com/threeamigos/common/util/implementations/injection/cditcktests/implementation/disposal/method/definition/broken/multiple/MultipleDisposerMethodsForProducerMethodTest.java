package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.multiple;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipleDisposerMethodsForProducerMethodTest {

    @Test
    void testMultipleDisposerMethodsForProducerMethodNotAllowed() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Bus.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Vehicle.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BusFactory.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
