package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.invocation;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorInvocationTest {

    @Test
    void testManagedBeanIsIntercepted() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();

            AlmightyInterceptor.reset();
            Missile missile = syringe.getBeanManager().createInstance().select(Missile.class).get();
            missile.fire();

            assertTrue(AlmightyInterceptor.methodIntercepted);
            assertNotNull(missile.getWarhead());

            AlmightyInterceptor.reset();
            Watcher watcher = syringe.getBeanManager().createInstance().select(Watcher.class).get();
            watcher.ping();

            assertTrue(AlmightyInterceptor.methodIntercepted);
            assertTrue(AlmightyInterceptor.lifecycleCallbackIntercepted);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInitializerMethodsNotIntercepted() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();

            AlmightyInterceptor.reset();
            Missile missile = syringe.getBeanManager().createInstance().select(Missile.class).get();

            assertFalse(AlmightyInterceptor.methodIntercepted);
            assertTrue(missile.initCalled());
            assertTrue(AlmightyInterceptor.methodIntercepted);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerMethodsAreIntercepted() {
        Syringe syringe = newSyringe();
        boolean requestActivated = false;
        try {
            syringe.start();
            requestActivated = syringe.activateRequestContextIfNeeded();

            AlmightyInterceptor.reset();
            syringe.getBeanManager().createInstance().select(Wheat.class).get();

            assertTrue(AlmightyInterceptor.methodIntercepted);
        } finally {
            if (requestActivated) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testDisposerMethodsAreIntercepted() {
        Syringe syringe = newSyringe();
        boolean requestActivated = false;
        try {
            syringe.start();
            requestActivated = syringe.activateRequestContextIfNeeded();

            AlmightyInterceptor.reset();
            WheatProducer.destroyed = false;

            Instance<Wheat> wheatInstance = syringe.getBeanManager().createInstance().select(Wheat.class);
            Wheat instance = wheatInstance.get();
            assertNotNull(instance);
            AlmightyInterceptor.methodIntercepted = false;
            wheatInstance.destroy(instance);

            assertTrue(WheatProducer.destroyed);
            assertTrue(AlmightyInterceptor.methodIntercepted);
        } finally {
            if (requestActivated) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodsAreIntercepted() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();

            AlmightyInterceptor.reset();
            MissileObserver.observed = false;
            syringe.getBeanManager().getEvent().select(Missile.class).fire(new Missile());

            assertTrue(MissileObserver.observed);
            assertTrue(AlmightyInterceptor.methodIntercepted);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testLifecycleCallbacksAreIntercepted() {
        Syringe syringe = newSyringe();
        boolean requestActivated = false;
        try {
            syringe.start();
            requestActivated = syringe.activateRequestContextIfNeeded();

            AlmightyInterceptor.reset();
            Rye rye = syringe.getBeanManager().createInstance().select(Rye.class).get();
            rye.ping();

            assertTrue(AlmightyInterceptor.methodIntercepted);
            assertTrue(AlmightyInterceptor.lifecycleCallbackIntercepted);
        } finally {
            if (requestActivated) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testObjectMethodsAreNotIntercepted() {
        Syringe syringe = newSyringe();
        try {
            syringe.start();

            AlmightyInterceptor.reset();
            syringe.getBeanManager().createInstance().select(Missile.class).get().toString();

            assertFalse(AlmightyInterceptor.methodIntercepted);
            assertTrue(AlmightyInterceptor.lifecycleCallbackIntercepted);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(AlmightyBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AlmightyInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Missile.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MissileObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Rye.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Warhead.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Watcher.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WheatProducer.class, BeanArchiveMode.EXPLICIT);
        return syringe;
    }
}
