package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.simple.definition;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class SimpleBeanDefinitionTest {

    @Test
    void testAbstractClassDeclaredInJavaNotDiscovered() {
        Syringe syringe = startContainer();
        try {
            assertEquals(0, getBeans(syringe.getBeanManager(), Cow_NotBean.class).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStaticInnerClassDeclaredInJavaAllowed() {
        Syringe syringe = startContainer();
        try {
            assertEquals(1, getBeans(syringe.getBeanManager(), OuterClass.StaticInnerClass.class).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonStaticInnerClassDeclaredInJavaNotDiscovered() {
        Syringe syringe = startContainer();
        try {
            assertEquals(0, getBeans(syringe.getBeanManager(), OuterClass.InnerClass_NotBean.class).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterfaceNotDiscoveredAsSimpleBean() {
        Syringe syringe = startContainer();
        try {
            assertEquals(0, getBeans(syringe.getBeanManager(), Car.class).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testExtensionNotDiscoveredAsSimpleBean() {
        Syringe syringe = startContainer();
        try {
            assertEquals(0, getBeans(syringe.getBeanManager(), SimpleExtension.class).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSimpleBeanOnlyIfConstructorParameterless() {
        Syringe syringe = startContainer();
        try {
            assertTrue(getBeans(syringe.getBeanManager(), Antelope_NotBean.class).isEmpty());
            assertFalse(getBeans(syringe.getBeanManager(), Donkey.class).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSimpleBeanOnlyIfConstructorIsInitializer() {
        Syringe syringe = startContainer();
        try {
            assertTrue(getBeans(syringe.getBeanManager(), Antelope_NotBean.class).isEmpty());
            assertFalse(getBeans(syringe.getBeanManager(), Sheep.class).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInitializerAnnotatedConstructor() {
        Syringe syringe = startContainer();
        try {
            Sheep.constructedCorrectly = false;
            getContextualReference(syringe.getBeanManager(), Sheep.class);
            assertTrue(Sheep.constructedCorrectly);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEmptyConstructorUsed() {
        Syringe syringe = startContainer();
        try {
            Donkey.constructedCorrectly = false;
            getContextualReference(syringe.getBeanManager(), Donkey.class);
            assertTrue(Donkey.constructedCorrectly);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInitializerAnnotatedConstructorUsedOverEmptyConstuctor() {
        Syringe syringe = startContainer();
        try {
            Turkey.constructedCorrectly = false;
            getContextualReference(syringe.getBeanManager(), Turkey.class);
            assertTrue(Turkey.constructedCorrectly);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDependentScopedBeanCanHaveNonStaticPublicField() {
        Syringe syringe = startContainer();
        try {
            assertEquals("pete", getContextualReference(syringe.getBeanManager(), Tiger.class).name);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSingletonScopedBeanCanHaveNonStaticPublicField() {
        Syringe syringe = startContainer();
        try {
            assertEquals("martin", getContextualReference(syringe.getBeanManager(), SnowTiger.class).name);
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Antelope_NotBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Car.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ClovenHoved.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cow_NotBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Donkey.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(OuterClass.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(OuterClass.StaticInnerClass.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(OuterClass.InnerClass_NotBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Sheep.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SimpleExtension.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SnowTiger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tiger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Turkey.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(White.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return (Set) beanManager.getBeans(type, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = (Set) beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
