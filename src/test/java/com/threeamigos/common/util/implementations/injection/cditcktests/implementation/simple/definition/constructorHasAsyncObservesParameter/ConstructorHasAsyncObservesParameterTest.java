package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.simple.definition.constructorHasAsyncObservesParameter;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstructorHasAsyncObservesParameterTest {

    @Test
    void testConstructorHasAsyncObservesParameter() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Food.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FoodConsumerBroken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
