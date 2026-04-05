package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processBeanAttributes.ignoreFinalMethods;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoreFinalMethodsTest {

    @Test
    void testAppDeployedAndBeanIsAvailable() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(IgnoringExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(Qux.class, BeanArchiveMode.EXPLICIT);

        try {
            syringe.start();
            Instance<Qux> quxInstance = syringe.getBeanManager().createInstance().select(Qux.class);
            assertFalse(quxInstance.isUnsatisfied());
            assertTrue(quxInstance.get().ping());
        } finally {
            syringe.shutdown();
        }
    }

}
