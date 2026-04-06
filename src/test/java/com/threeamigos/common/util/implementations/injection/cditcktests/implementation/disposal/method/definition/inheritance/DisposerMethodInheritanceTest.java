package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.inheritance;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisposerMethodInheritanceTest {

    @BeforeEach
    void resetState() {
        Apple.disposedBy.clear();
        Meal.disposedBy.clear();
    }

    @Test
    void testManagedBeanDisposerMethodNotInherited() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Apple.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AppleTree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GrannySmithAppleTree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GreatGrannySmithAppleTree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Meal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cook.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Chef.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Yummy.class, BeanArchiveMode.EXPLICIT);
        syringe.start();

        try {
            BeanManager beanManager = syringe.getBeanManager();

            DependentInstance<Apple> appleInstance = newDependentInstance(beanManager, Apple.class);
            Apple apple = appleInstance.get();
            assertEquals(GreatGrannySmithAppleTree.class, apple.getTree().getClass());
            appleInstance.destroy();
            assertEquals(0, Apple.disposedBy.size());

            DependentInstance<Meal> mealInstance = newDependentInstance(beanManager, Meal.class);
            Meal meal = mealInstance.get();
            assertEquals(Chef.class, meal.getCook().getClass());
            mealInstance.destroy();
            assertEquals(0, Meal.disposedBy.size());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> DependentInstance<T> newDependentInstance(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = resolveBean(beanManager, type);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        T instance = bean.create(creationalContext);
        return new DependentInstance<T>(bean, creationalContext, instance);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static class DependentInstance<T> {

        private final Bean<T> bean;
        private final CreationalContext<T> creationalContext;
        private final T instance;

        private DependentInstance(Bean<T> bean, CreationalContext<T> creationalContext, T instance) {
            this.bean = bean;
            this.creationalContext = creationalContext;
            this.instance = instance;
        }

        private T get() {
            return instance;
        }

        private void destroy() {
            bean.destroy(instance, creationalContext);
        }
    }
}
