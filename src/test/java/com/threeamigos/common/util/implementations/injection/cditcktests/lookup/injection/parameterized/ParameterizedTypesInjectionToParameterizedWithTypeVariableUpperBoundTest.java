package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParameterizedTypesInjectionToParameterizedWithTypeVariableUpperBoundTest {

    @Test
    void testInjection() {
        Syringe syringe = newSyringe();
        try {
            ConsumerTypeVariableUpperBound<?, ?> consumer = syringe.getBeanManager().createInstance()
                    .select(new TypeLiteral<ConsumerTypeVariableUpperBound<?, ?>>() {
                    })
                    .get();
            assertNotNull(consumer.getDao());
            assertEquals(Dao.class.getName(), consumer.getDao().getId());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Dao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ObjectDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerTypeVariableUpperBound.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
