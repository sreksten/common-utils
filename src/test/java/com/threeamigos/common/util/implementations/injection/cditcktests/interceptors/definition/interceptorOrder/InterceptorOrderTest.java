package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition.interceptorOrder;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorOrderTest {

    @Test
    void testInterceptorsInvocationOrder() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Secure.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FirstInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SecondInterceptor.class, BeanArchiveMode.EXPLICIT);
        try {
            syringe.start();
            Foo foo = syringe.getBeanManager().createInstance().select(Foo.class).get();
            assertNotNull(foo);

            ActionSequence.reset();
            foo.bar();

            List<String> sequence = ActionSequence.getSequenceData();
            assertEquals(2, sequence.size());
            assertEquals(SecondInterceptor.class.getName(), sequence.get(0));
            assertEquals(FirstInterceptor.class.getName(), sequence.get(1));
        } finally {
            syringe.shutdown();
        }
    }
}
