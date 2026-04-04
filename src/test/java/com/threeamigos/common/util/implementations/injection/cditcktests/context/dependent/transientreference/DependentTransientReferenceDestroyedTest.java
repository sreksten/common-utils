package com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference.dependenttransientreferencedestroyedtest.test.ActionSequence;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference.dependenttransientreferencedestroyedtest.test.Kitchen;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference.dependenttransientreferencedestroyedtest.test.Meal;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference.dependenttransientreferencedestroyedtest.test.Spoon;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference.dependenttransientreferencedestroyedtest.test.Util;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class DependentTransientReferenceDestroyedTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.transientreference.dependenttransientreferencedestroyedtest.test";

    @Test
    void testConstructorAndInitializer() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            ActionSequence.reset();
            DependentInstance<Spoon> spoonInstance = new DependentInstance<Spoon>(syringe.getBeanManager(), Spoon.class);
            spoonInstance.get().ping();

            assertEquals(2, ActionSequence.getSequenceSize());
            ActionSequence.getSequence().assertDataContainsAll(
                    Util.buildOwnerId(Spoon.class, true, Util.TYPE_CONSTRUCTOR),
                    Util.buildOwnerId(Spoon.class, true, Util.TYPE_INIT)
            );

            ActionSequence.reset();
            spoonInstance.destroy();
            assertEquals(2, ActionSequence.getSequenceSize());
            ActionSequence.getSequence().assertDataContainsAll(
                    Util.buildOwnerId(Spoon.class, false, Util.TYPE_CONSTRUCTOR),
                    Util.buildOwnerId(Spoon.class, false, Util.TYPE_INIT)
            );
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerMethod() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            ActionSequence.reset();

            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                DependentInstance<Meal> mealInstance = new DependentInstance<Meal>(syringe.getBeanManager(), Meal.class);
                Meal meal = mealInstance.get();
                assertEquals("soup", meal.getName());

                assertEquals(1, ActionSequence.getSequenceSize());
                ActionSequence.getSequence().assertDataContainsAll(
                        Util.buildOwnerId(Kitchen.class, true, Util.TYPE_PRODUCER)
                );

                mealInstance.destroy();
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    private static class DependentInstance<T> {

        private final Bean<T> bean;
        private final CreationalContext<T> creationalContext;
        private final T instance;
        private boolean destroyed;

        @SuppressWarnings({"unchecked", "rawtypes"})
        private DependentInstance(BeanManager beanManager, Class<T> beanType, Annotation... qualifiers) {
            Set<Bean<?>> beans = beanManager.getBeans(beanType, qualifiers);
            bean = (Bean<T>) beanManager.resolve((Set) beans);
            if (!bean.getScope().equals(Dependent.class)) {
                throw new IllegalStateException("Bean is not dependent");
            }
            creationalContext = beanManager.createCreationalContext(bean);
            instance = bean.create(creationalContext);
        }

        private T get() {
            if (destroyed) {
                throw new IllegalStateException("Instance already destroyed");
            }
            return instance;
        }

        private void destroy() {
            if (destroyed) {
                throw new IllegalStateException("Instance already destroyed");
            }
            bean.destroy(instance, creationalContext);
            destroyed = true;
        }
    }
}
