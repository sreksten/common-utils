package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.parameterAnnotatedDisposes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterAnnotatedDisposesTest {

    @Test
    void testInitializerMethodHasParameterAnnotatedDisposes() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(ChickenHutch.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Capercaillie_Broken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
