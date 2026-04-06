package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.parameterAnnotatedAsyncObserves;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterAnnotatedAsyncObservesTest {

    @Test
    void testInitializerMethodHasParameterAnnotatedAsyncObserves() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Food.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FoodConsumer.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
