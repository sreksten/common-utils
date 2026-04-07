package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.order.aroundConstruct;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class AroundConstructOrderTest {

    @Test
    void testInvocationOrder() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            ActionSequence.reset();
            getContextualReference(syringe, Foo.class);
            ActionSequence.assertSequenceDataEquals(SuperInterceptor1.class, MiddleInterceptor1.class,
                    Interceptor1.class, Interceptor2.class, Interceptor3.class, Interceptor4.class);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Foo.class,
                FooClassBinding.class,
                FooCtorBinding.class,
                Interceptor1.class,
                Interceptor2.class,
                Interceptor3.class,
                Interceptor4.class,
                MiddleInterceptor1.class,
                SuperInterceptor1.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }
}
