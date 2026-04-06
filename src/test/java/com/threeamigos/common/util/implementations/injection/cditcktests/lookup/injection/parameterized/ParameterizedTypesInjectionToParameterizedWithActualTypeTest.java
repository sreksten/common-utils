package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParameterizedTypesInjectionToParameterizedWithActualTypeTest {

    @Test
    void testInjection() {
        Syringe syringe = newSyringe();
        try {
            ConsumerActualType consumer = syringe.getBeanManager().createInstance().select(ConsumerActualType.class).get();

            assertNotNull(consumer.getDao());
            assertEquals(Dao.class.getName(), consumer.getDao().getId());

            assertNotNull(consumer.getIntegerStringDao());
            assertEquals(IntegerStringDao.class.getName(), consumer.getIntegerStringDao().getId());

            assertNotNull(consumer.getIntegerListOfStringsDao());
            assertEquals(IntegerListOfStringsDao.class.getName(), consumer.getIntegerListOfStringsDao().getId());

            assertNotNull(consumer.getObjectDao());
            assertEquals(Dao.class.getName(), consumer.getObjectDao().getId());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Dao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerStringDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerListOfStringsDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerActualType.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerPowered.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
