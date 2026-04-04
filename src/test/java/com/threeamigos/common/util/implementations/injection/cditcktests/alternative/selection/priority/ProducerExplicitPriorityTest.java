package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProducerExplicitPriorityTest {

    private static final String ALT = "alternative";
    private static final String ALT2 = "alternative2";

    private static final String TEST_ALTERNATIVE_PRODUCER_WITH_PRIORITY_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority";
    private static final String TEST_PRIORITY_ON_PRODUCER_OVER_PRIORITY_ON_CLASS_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass";

    @Test
    void testAlternativeProducerWithPriority() {
        Syringe syringe = new Syringe(TEST_ALTERNATIVE_PRODUCER_WITH_PRIORITY_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority.Probe probe =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority.Probe.class);

            assertNotNull(probe);
            assertEquals(ALT, probe.alphaMethodProducer().ping());
            assertEquals(ALT, probe.alphaFieldProducer().ping());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPriorityOnProducerOverPriorityOnClass() {
        Syringe syringe = new Syringe(TEST_PRIORITY_ON_PRODUCER_OVER_PRIORITY_ON_CLASS_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass.Probe probe =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass.Probe.class);

            assertNotNull(probe);
            assertEquals(ALT2, probe.betaMethodProducer().ping());
            assertEquals(ALT2, probe.betaFieldProducer().ping());
            assertEquals(ALT2, probe.gammaMethodProducer().ping());
            assertEquals(ALT2, probe.gammaFieldProducer().ping());
            assertEquals(ALT2, probe.deltaMethodProducer().ping());
            assertEquals(ALT2, probe.deltaFieldProducer().ping());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
