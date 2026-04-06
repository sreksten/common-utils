package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.multiParams;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipleDisposeParametersDefinitionTest {

    @Test
    void testMultipleDisposeParameters() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Spider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderProducer_Broken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
