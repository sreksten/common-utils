package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.bindings.aroundConstruct;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ConstructorInterceptionTest {

    @Test
    void testConstructorLevelBinding() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, BeanWithConstructorLevelBinding.class);
            ActionSequence.assertSequenceDataEquals(AlphaInterceptor2.class, BeanWithConstructorLevelBinding.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMultipleConstructorLevelBinding() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, BeanWithMultipleConstructorLevelBinding.class);
            ActionSequence.assertSequenceDataEquals(
                    AlphaInterceptor2.class,
                    BravoInterceptor.class,
                    BeanWithMultipleConstructorLevelBinding.class
            );
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testTypeLevelBinding() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, BeanWithTypeLevelBinding.class);
            ActionSequence.assertSequenceDataEquals(AlphaInterceptor1.class, BeanWithTypeLevelBinding.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testTypeLevelAndConstructorLevelBinding() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, BeanWithConstructorLevelAndTypeLevelBinding.class);
            ActionSequence.assertSequenceDataEquals(
                    AlphaInterceptor1.class,
                    BravoInterceptor.class,
                    BeanWithConstructorLevelAndTypeLevelBinding.class
            );
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testOverridingTypeLevelBinding() {
        ActionSequence.reset();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, BeanOverridingTypeLevelBinding.class);
            ActionSequence.assertSequenceDataEquals(AlphaInterceptor2.class, BeanOverridingTypeLevelBinding.class);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AbstractInterceptor.class,
                AlphaBinding.class,
                AlphaInterceptor1.class,
                AlphaInterceptor2.class,
                BeanOverridingTypeLevelBinding.class,
                BeanWithConstructorLevelAndTypeLevelBinding.class,
                BeanWithConstructorLevelBinding.class,
                BeanWithMultipleConstructorLevelBinding.class,
                BeanWithTypeLevelBinding.class,
                BravoBinding.class,
                BravoInterceptor.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }
}
