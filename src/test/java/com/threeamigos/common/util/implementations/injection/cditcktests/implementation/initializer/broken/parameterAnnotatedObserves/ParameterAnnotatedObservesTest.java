package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.parameterAnnotatedObserves;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterAnnotatedObservesTest {

    @Test
    void testInitializerMethodHasParameterAnnotatedObserves() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(DangerCall.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Grouse_Broken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
