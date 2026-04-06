package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InitializerMethodTest {

    @Test
    void testBindingTypeOnInitializerParameter() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            PremiumChickenHutch hutch = resolveReference(beanManager, PremiumChickenHutch.class);
            assertEquals("Preferred", hutch.getChicken().getName());

            StandardChickenHutch anotherHutch = resolveReference(beanManager, StandardChickenHutch.class);
            assertEquals("Standard", anotherHutch.getChicken().getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMultipleInitializerMethodsAreCalled() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            ChickenHutch chickenHutch = resolveReference(beanManager, ChickenHutch.class);
            assertNotNull(chickenHutch.fox);
            assertNotNull(chickenHutch.chicken);
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Chicken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ChickenHutch.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ChickenInterface.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Fox.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Preferred.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PreferredChicken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PremiumChickenHutch.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StandardChicken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StandardChickenHutch.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StandardVariety.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = resolveBean(beanManager, type);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
