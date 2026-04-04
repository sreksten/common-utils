package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.stereotype.priority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StereotypeWithAlternativeSelectedByPriorityTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.stereotype.priority.stereotypewithalternativeselectedbyprioritytest.teststereotypealternativeisenabled";

    @Test
    void testStereotypeAlternativeIsEnabled() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.stereotype.priority.stereotypewithalternativeselectedbyprioritytest.teststereotypealternativeisenabled.SomeInterface ref =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.stereotype.priority.stereotypewithalternativeselectedbyprioritytest.teststereotypealternativeisenabled.SomeInterface.class);
            assertEquals("AlternativeImpl", ref.ping());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
