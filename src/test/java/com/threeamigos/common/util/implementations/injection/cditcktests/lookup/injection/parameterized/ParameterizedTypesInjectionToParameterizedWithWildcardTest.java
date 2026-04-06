package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParameterizedTypesInjectionToParameterizedWithWildcardTest {

    @Test
    void testInjection() {
        Syringe syringe = newSyringe();
        try {
            ConsumerWildcard consumer = syringe.getBeanManager().createInstance().select(ConsumerWildcard.class).get();

            assertNotNull(consumer.getDao());
            assertEquals(Dao.class.getName(), consumer.getDao().getId());

            assertNotNull(consumer.getIntegerStringDao());
            assertEquals(IntegerStringDao.class.getName(), consumer.getIntegerStringDao().getId());

            assertNotNull(consumer.getNumberDao());
            assertEquals(NumberDao.class.getName(), consumer.getNumberDao().getId());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Dao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerStringDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StringDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NumberDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerPowered.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerWildcard.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
