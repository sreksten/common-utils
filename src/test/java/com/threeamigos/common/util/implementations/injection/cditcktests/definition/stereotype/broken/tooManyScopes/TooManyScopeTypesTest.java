package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.tooManyScopes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.tooManyScopes.toomanyscopetypestest.test.Elk_Broken;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.tooManyScopes.toomanyscopetypestest.test.StereotypeWithTooManyScopeTypes_Broken;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TooManyScopeTypesTest {

    @Test
    void testStereotypeWithTooManyScopeTypes() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Elk_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StereotypeWithTooManyScopeTypes_Broken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
