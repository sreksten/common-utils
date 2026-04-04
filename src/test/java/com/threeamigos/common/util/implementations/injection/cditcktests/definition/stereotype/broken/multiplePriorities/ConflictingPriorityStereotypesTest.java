package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.conflictingprioritystereotypestest.test.PriorityStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.conflictingprioritystereotypestest.test.PriorityStereotype2;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.conflictingprioritystereotypestest.test.SomeBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConflictingPriorityStereotypesTest {

    @Test
    void testConflictingPrioritiesFromStereotypes() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(SomeBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PriorityStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PriorityStereotype2.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
