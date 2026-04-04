package com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.repeatable;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.repeatable.repeatablequalifierstest.test.Probe;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.repeatable.repeatablequalifierstest.test.Process;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.repeatable.repeatablequalifierstest.test.ProcessObserver;
import com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.repeatable.repeatablequalifierstest.test.Start;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepeatableQualifiersTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.definition.qualifier.repeatable.repeatablequalifierstest.test";

    private static final Start.StartLiteral A = new Start.StartLiteral("A");
    private static final Start.StartLiteral B = new Start.StartLiteral("B");
    private static final Start.StartLiteral C = new Start.StartLiteral("C");

    @Test
    void resolutionWithRepeatableQualifiers() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            Probe probe = resolveReference(syringe.getBeanManager(), Probe.class);
            Instance<Process> processInstance = probe.getProcessInstance();
            ProcessObserver observer = probe.getObserver();

            assertEquals(4L, processInstance.stream().count());
            assertTrue(processInstance.select(A).isResolvable());
            assertFalse(processInstance.select(B).isResolvable());
            assertTrue(processInstance.select(C).isResolvable());
            assertTrue(processInstance.select(B, C).isResolvable());

            assertEquals(1, observer.getProcessAObserved());
            assertEquals(2, observer.getProcessBObserved());
            assertEquals(1, observer.getProcessABObserved());

            assertEquals(3, observer.getProcessAMetadata().getQualifiers().size());
            assertTrue(observer.getProcessAMetadata().getQualifiers().contains(A));

            assertEquals(4, observer.getProcessBCMetadata().getQualifiers().size());
            assertTrue(observer.getProcessBCMetadata().getQualifiers().contains(B));
            assertTrue(observer.getProcessBCMetadata().getQualifiers().contains(C));
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        return (T) beanManager.getReference(beanManager.resolve((java.util.Set) beanManager.getBeans(type)),
                type,
                beanManager.createCreationalContext(null));
    }
}
