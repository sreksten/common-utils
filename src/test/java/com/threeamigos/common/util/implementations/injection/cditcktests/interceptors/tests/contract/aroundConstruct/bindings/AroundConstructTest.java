package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.aroundConstruct.bindings;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class AroundConstructTest {

    @Test
    void testInterceptorInvocation() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, Alpha.class);
            ActionSequence.assertSequenceDataEquals(AlphaInterceptor.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testReplacingParameters() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Bravo bravo = getContextualReference(syringe, Bravo.class);
            assertNotNull(bravo.getParameter());
            assertEquals(BravoInterceptor.NEW_PARAMETER_VALUE, bravo.getParameter().getValue());
            ActionSequence.assertSequenceDataEquals(BravoInterceptor.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testExceptions() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertThrows(CharlieException.class, () -> getContextualReference(syringe, Charlie.class));
            ActionSequence.assertSequenceDataEquals(CharlieInterceptor2.class, CharlieInterceptor1.class);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AbstractInterceptor.class,
                Alpha.class,
                AlphaBinding.class,
                AlphaInterceptor.class,
                Bravo.class,
                BravoBinding.class,
                BravoInterceptor.class,
                BravoParameter.class,
                BravoParameterProducer.class,
                Charlie.class,
                CharlieBinding.class,
                CharlieException.class,
                CharlieInterceptor1.class,
                CharlieInterceptor2.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
