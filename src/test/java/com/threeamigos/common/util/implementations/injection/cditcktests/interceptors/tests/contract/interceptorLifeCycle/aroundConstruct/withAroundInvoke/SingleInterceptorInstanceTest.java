package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.interceptorLifeCycle.aroundConstruct.withAroundInvoke;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SingleInterceptorInstanceTest {

    @Test
    void testAroundConstructInvokedAfterDependencyInjectionOnInterceptorClasses() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Foo foo = getContextualReference(syringe, Foo.class);
            assertEquals(2, foo.ping());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Foo.class,
                FooBinding.class,
                FooInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
