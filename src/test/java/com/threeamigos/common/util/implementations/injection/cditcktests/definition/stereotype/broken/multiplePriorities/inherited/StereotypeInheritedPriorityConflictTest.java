package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test.AnotherDumbStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test.AnotherStereotypeWithPriority;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test.DumbStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test.Foo;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test.FooAlternative;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test.FooAncestor;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.inherited.stereotypeinheritedpriorityconflicttest.test.StereotypeWithPriority;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StereotypeInheritedPriorityConflictTest {

    @Test
    void testInheritedStereotypesAreConflicting() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(FooAlternative.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FooAncestor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DumbStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StereotypeWithPriority.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnotherDumbStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnotherStereotypeWithPriority.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
