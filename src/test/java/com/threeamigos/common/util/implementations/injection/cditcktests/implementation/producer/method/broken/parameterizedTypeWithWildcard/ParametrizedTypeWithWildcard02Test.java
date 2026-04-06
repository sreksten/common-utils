package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.producer.method.broken.parameterizedTypeWithWildcard;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParametrizedTypeWithWildcard02Test {

    @Test
    void testParameterizedReturnTypeWithDoubleWildcard() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Spiderman.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpidermanProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FunnelWeaver.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
