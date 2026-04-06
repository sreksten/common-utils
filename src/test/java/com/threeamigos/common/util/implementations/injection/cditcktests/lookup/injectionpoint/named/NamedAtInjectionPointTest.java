package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injectionpoint.named;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NamedAtInjectionPointTest {

    @Test
    void testFieldNameUsedAsBeanName() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
        beanManager.getContextManager().activateRequest();
        try {
            FishingNet fishingNet = syringe.getBeanManager().createInstance().select(FishingNet.class).get();
            Pike pike = syringe.getBeanManager().createInstance().select(Pike.class).get();

            assertNotNull(fishingNet);
            assertEquals(Integer.valueOf(1), fishingNet.getCarp().ping());

            assertNotNull(pike);
            assertNotNull(pike.getDaphnia());
            assertEquals(DaphniaProducer.NAME, pike.getDaphnia().getName());
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Carp.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Daphnia.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DaphniaProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FishingNet.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Pike.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
