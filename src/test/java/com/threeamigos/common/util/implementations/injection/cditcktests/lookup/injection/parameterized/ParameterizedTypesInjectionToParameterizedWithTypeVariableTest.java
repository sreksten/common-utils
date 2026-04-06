package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParameterizedTypesInjectionToParameterizedWithTypeVariableTest {

    @Test
    void testInjection() {
        Syringe syringe = newSyringe();
        try {
            ConsumerTypeVariable<?, ?> consumer = syringe.getBeanManager().createInstance()
                    .select(new TypeLiteral<ConsumerTypeVariable<?, ?>>() {
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
        syringe.addDiscoveredClass(StringDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerTypeVariable.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
