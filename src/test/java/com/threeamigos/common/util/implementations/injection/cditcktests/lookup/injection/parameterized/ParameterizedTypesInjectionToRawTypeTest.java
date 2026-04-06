package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injection.parameterized;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParameterizedTypesInjectionToRawTypeTest {

    @Test
    void testInjection() {
        Syringe syringe = newSyringe();
        try {
            ConsumerRaw consumer = syringe.getBeanManager().createInstance().select(ConsumerRaw.class).get();
            ConsumerRawObject consumerObject = syringe.getBeanManager().createInstance().select(ConsumerRawObject.class).get();

            assertNotNull(consumer);
            assertNotNull(consumer.getDao());
            assertEquals(Dao.class.getName(), consumer.getDao().getId());

            assertNotNull(consumerObject);
            assertNotNull(consumerObject.getDao());
            assertEquals(DaoProducer.class.getName(), consumerObject.getDao().getId());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Dao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DaoProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NumberDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StringDao.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerRaw.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConsumerRawObject.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ObjectPowered.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
