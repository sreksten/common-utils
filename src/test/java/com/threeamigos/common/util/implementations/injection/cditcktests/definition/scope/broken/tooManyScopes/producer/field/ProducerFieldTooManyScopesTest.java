package com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.broken.tooManyScopes.producer.field;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.broken.tooManyScopes.producer.field.producerfieldtoomanyscopestest.test.ProducerFieldWithTooManyScopeTypes_Broken;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProducerFieldTooManyScopesTest {

    @Test
    void testTooManyScopesSpecifiedInJava() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(ProducerFieldWithTooManyScopeTypes_Broken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
