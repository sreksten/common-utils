package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.conflictingprioritiesfromsinglestereotypetest.test.AnotherPriorityStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.conflictingprioritiesfromsinglestereotypetest.test.PriorityStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.conflictingprioritiesfromsinglestereotypetest.test.SomeOtherBean;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConflictingPrioritiesFromSingleStereotypeTest {

    @Test
    void testConflictingPrioritiesFromStereotype() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(SomeOtherBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnotherPriorityStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PriorityStereotype.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
