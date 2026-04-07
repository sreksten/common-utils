package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.lifecycleCallback.exceptions;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class LifecycleCallbackInterceptorExceptionTest {

    @Test
    void testPostConstructCanThrowRuntimeException() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertThrows(RuntimeException.class, () -> getContextualReference(syringe, Sheep.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testLifecycleCallbackInterceptorCanCatchException() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, Goat.class);
            assertTrue(GoatInterceptor.isExceptionCaught());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPreDestroyNotInvokedWhenInstanceDiscarded() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertThrows(RuntimeException.class, () -> getContextualReference(syringe, Cat.class));
            assertFalse(CatInterceptor.preDestroyCalled);
            assertFalse(Cat.preDestroyCalled);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Cat.class,
                CatBinding.class,
                CatInterceptor.class,
                Goat.class,
                GoatBinding.class,
                GoatInterceptor.class,
                Sheep.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }
}
