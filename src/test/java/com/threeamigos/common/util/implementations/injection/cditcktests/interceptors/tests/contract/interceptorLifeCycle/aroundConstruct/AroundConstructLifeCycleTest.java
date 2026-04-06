package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.interceptorLifeCycle.aroundConstruct;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class AroundConstructLifeCycleTest {

    @Test
    void testAroundConstructInvokedAfterDependencyInjectionOnInterceptorClasses() {
        resetState();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, Foo.class);
            assertTrue(FooCommonInterceptor.commonAroundConstructCalled);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInstanceNotCreatedUnlessInvocationContextProceedCalled() {
        resetState();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            Baz2Interceptor.setProceed(false);
            assertFalse(Baz.postConstructedCalled,
                    "Instance created even though InvocationContext.proceed() was not called.");

            Baz2Interceptor.setProceed(true);
            Baz baz = getContextualReference(syringe, Baz.class);
            assertNotNull(baz, "Instance not created even though InvocationContext.proceed() was called.");
            assertTrue(baz.accessed);
            assertTrue(baz.injectionPerformedCorrectly());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bar.class,
                Baz.class,
                Baz1Interceptor.class,
                Baz2Interceptor.class,
                BazBinding.class,
                Foo.class,
                FooBinding.class,
                FooCommonInterceptor.class,
                FooInterceptor.class,
                FooSuperInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }

    private void resetState() {
        FooCommonInterceptor.reset();
        FooSuperInterceptor.bar = null;
        FooInterceptor.number = 0;
        Baz.postConstructedCalled = false;
        Baz2Interceptor.setProceed(true);
    }
}
