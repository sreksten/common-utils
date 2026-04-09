package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injectionpoint.requiredtype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LegalRequiredTypeTest {

    @Test
    void testLegalRequiredType() {
        Syringe syringe = newSyringe();
        try {
            Forest forest = syringe.getBeanManager().createInstance().select(Forest.class).get();
            forest.ping();
            assertEquals(10, forest.getAge());
            assertNotNull(forest.getNeedles());
            assertEquals(1, forest.getNeedles().length);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Forest.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Conifer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Leaf.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Needle.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spruce.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tree.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
