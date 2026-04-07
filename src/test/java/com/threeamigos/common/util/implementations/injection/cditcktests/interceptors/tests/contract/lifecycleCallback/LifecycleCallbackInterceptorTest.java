package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.lifecycleCallback;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class LifecycleCallbackInterceptorTest {

    @Test
    void testPostConstructInterceptor() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                getContextualReference(syringe, Goat.class);
                assertTrue(Goat.isPostConstructInterceptorCalled());
                assertTrue(AnimalInterceptor.isPostConstructInterceptorCalled(Goat.GOAT));

                getContextualReference(syringe, Hen.class).toString();
                assertTrue(Hen.isPostConstructInterceptorCalled());
                assertTrue(AnimalInterceptor.isPostConstructInterceptorCalled(Hen.HEN));

                getContextualReference(syringe, Cow.class).toString();
                assertTrue(Cow.isPostConstructInterceptorCalled());
                assertTrue(AnimalInterceptor.isPostConstructInterceptorCalled(Cow.COW));
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPreDestroyInterceptor() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            createAndDestroyInstance(syringe, Goat.class);
            assertTrue(Goat.isPreDestroyInterceptorCalled());
            assertTrue(AnimalInterceptor.isPreDestroyInterceptorCalled(Goat.GOAT));

            createAndDestroyInstance(syringe, Hen.class);
            assertTrue(Hen.isPreDestroyInterceptorCalled());
            assertTrue(AnimalInterceptor.isPreDestroyInterceptorCalled(Hen.HEN));

            createAndDestroyInstance(syringe, Cow.class);
            assertTrue(Cow.isPreDestroyInterceptorCalled());
            assertTrue(AnimalInterceptor.isPreDestroyInterceptorCalled(Cow.COW));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSingleMethodInterposingMultipleLifecycleCallbackEvents() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            AlmightyLifecycleInterceptor.reset();
            Dog.reset();
            createAndDestroyInstance(syringe, Dog.class);
            assertEquals(3, AlmightyLifecycleInterceptor.getNumberOfInterceptions());
            assertEquals(2, Dog.getNumberOfInterceptions());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAroundInvokeAndLifeCycleCallbackInterceptorsCanBeDefinedOnTheSameClass() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals("foofoo", getContextualReference(syringe, Goat.class).echo("foo"));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPublicLifecycleInterceptorMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, Chicken.class);
            assertTrue(PublicLifecycleInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProtectedLifecycleInterceptorMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, Chicken.class);
            assertTrue(ProtectedLifecycleInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPrivateLifecycleInterceptorMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, Chicken.class);
            assertTrue(PrivateLifecycleInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPackagePrivateLifecycleInterceptorMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, Chicken.class);
            assertTrue(PackagePrivateLifecycleInterceptor.isIntercepted());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testLifeCycleCallbackInterceptorNotInvokedForMethodLevelInterceptor() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals("bar", getContextualReference(syringe, Sheep.class).foo());
            assertTrue(SheepInterceptor.isAroundInvokeCalled());
            assertFalse(SheepInterceptor.isPostConstructCalled());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AlmightyLifecycleInterceptor.class,
                Animal.class,
                AnimalBinding.class,
                AnimalInterceptor.class,
                Bar.class,
                Chicken.class,
                ChickenBinding.class,
                Cow.class,
                Dog.class,
                DogBinding.class,
                Goat.class,
                Hen.class,
                PackagePrivateLifecycleInterceptor.class,
                PrivateLifecycleInterceptor.class,
                ProtectedLifecycleInterceptor.class,
                PublicLifecycleInterceptor.class,
                Sheep.class,
                SheepBinding.class,
                SheepInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    @SuppressWarnings("unchecked")
    private <T extends Animal> void createAndDestroyInstance(Syringe syringe, Class<T> clazz) {
        boolean activatedRequest = syringe.activateRequestContextIfNeeded();
        try {
            Bean<T> bean = getUniqueBean(syringe, clazz);
            CreationalContext<T> ctx = syringe.getBeanManager().createCreationalContext(bean);
            T instance = clazz.cast(syringe.getBeanManager().getReference(bean, clazz, ctx));
            instance.foo();
            bean.destroy(instance, ctx);
        } finally {
            if (activatedRequest) {
                syringe.deactivateRequestContextIfActive();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getUniqueBean(Syringe syringe, Class<T> beanClass) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(beanClass);
        assertEquals(1, beans.size());
        return (Bean<T>) beans.iterator().next();
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }
}
