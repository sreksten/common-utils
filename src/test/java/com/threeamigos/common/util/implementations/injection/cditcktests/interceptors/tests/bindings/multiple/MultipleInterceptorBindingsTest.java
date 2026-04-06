package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.bindings.multiple;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class MultipleInterceptorBindingsTest {

    @Test
    void testInterceptorAppliedToBeanWithAllBindings() {
        MissileInterceptor.intercepted = false;
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Missile missile = getContextualReference(syringe, FastAndDeadlyMissile.class);
            missile.fire();
            assertTrue(MissileInterceptor.intercepted);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorNotAppliedToBeanWithSomeBindings() {
        MissileInterceptor.intercepted = false;
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Missile missile = getContextualReference(syringe, SlowMissile.class);
            missile.fire();
            assertFalse(MissileInterceptor.intercepted);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMultipleInterceptorsOnMethod() {
        LockInterceptor.intercepted = false;
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            GuidedMissile bullet = getContextualReference(syringe, GuidedMissile.class);
            bullet.fire();
            assertFalse(LockInterceptor.intercepted);
            bullet.lockAndFire();
            assertTrue(LockInterceptor.intercepted);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Deadly.class,
                Fast.class,
                FastAndDeadlyMissile.class,
                GuidedMissile.class,
                LockInterceptor.class,
                Missile.class,
                MissileInterceptor.class,
                Slow.class,
                SlowMissile.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
