package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.scopeConflict;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.scopeConflict.incompatiblestereotypestest.test.AnimalStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.scopeConflict.incompatiblestereotypestest.test.FishStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.scopeConflict.incompatiblestereotypestest.test.Scallop_Broken;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class IncompatibleStereotypesTest {

    @Test
    void testMultipleIncompatibleScopeStereotypes() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Scallop_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnimalStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FishStereotype.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
