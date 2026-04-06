package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.simple.definition.normalScopedWithPublicStaticField;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class NormalScopedWithPublicStaticFieldTest {

    @Test
    void testNormalScopedBeanCanHavePublicStaticField() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Leopard.class, BeanArchiveMode.EXPLICIT);
        try {
            syringe.start();
            assertEquals("john", Leopard.NAME);
        } finally {
            syringe.shutdown();
        }
    }
}
