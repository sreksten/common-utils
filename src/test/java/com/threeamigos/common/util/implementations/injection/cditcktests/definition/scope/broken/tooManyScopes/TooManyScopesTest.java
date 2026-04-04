package com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.broken.tooManyScopes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.scope.broken.tooManyScopes.toomanyscopestest.test.BeanWithTooManyScopeTypes_Broken;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TooManyScopesTest {

    @Test
    void testTooManyScopesSpecifiedInJava() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(BeanWithTooManyScopeTypes_Broken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
