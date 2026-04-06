package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.manager;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerTest {

    @Test
    void testInjectingManager() {
        Syringe syringe = newSyringe();
        try {
            FishFarmOffice fishFarmOffice = getContextualReference(syringe, FishFarmOffice.class);
            assertNotNull(fishFarmOffice.beanManager);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testContainerProvidesManagerBean() {
        Syringe syringe = newSyringe();
        try {
            assertTrue(getBeans(syringe, BeanManager.class).size() > 0);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testManagerBeanIsDependentScoped() {
        Syringe syringe = newSyringe();
        try {
            Bean<BeanManager> beanManager = getBeans(syringe, BeanManager.class).iterator().next();
            assertEquals(Dependent.class, beanManager.getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testManagerBeanHasCurrentBinding() {
        Syringe syringe = newSyringe();
        try {
            Bean<BeanManager> beanManager = getBeans(syringe, BeanManager.class).iterator().next();
            assertTrue(beanManager.getQualifiers().contains(Default.Literal.INSTANCE));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetReferenceReturnsContextualInstance() {
        Syringe syringe = newSyringe();
        try {
            Bean<FishFarmOffice> bean = getBeans(syringe, FishFarmOffice.class).iterator().next();
            Object reference = syringe.getBeanManager().getReference(bean, FishFarmOffice.class,
                    syringe.getBeanManager().createCreationalContext(bean));
            assertInstanceOf(FishFarmOffice.class, reference);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetReferenceWithIllegalBeanType() {
        Syringe syringe = newSyringe();
        try {
            Bean<FishFarmOffice> bean = getBeans(syringe, FishFarmOffice.class).iterator().next();
            assertThrows(IllegalArgumentException.class,
                    () -> syringe.getBeanManager().getReference(bean, BigDecimal.class,
                            syringe.getBeanManager().createCreationalContext(bean)));
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(FishFarmOffice.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(Syringe syringe, Class<T> beanType, Annotation... qualifiers) {
        return (Set) syringe.getBeanManager().getBeans(beanType, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(Syringe syringe, Class<T> beanType, Annotation... qualifiers) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(beanType, qualifiers);
        Bean<T> bean = (Bean<T>) syringe.getBeanManager().resolve((Set) beans);
        return (T) syringe.getBeanManager().getReference(bean, beanType,
                syringe.getBeanManager().createCreationalContext(bean));
    }
}
