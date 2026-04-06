package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.simple.definition.constructorHasDisposesParameter;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstructorHasDisposesParameterTest {

    @Test
    void testConstructorHasDisposesParameter() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(DisposingConstructor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Duck.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
