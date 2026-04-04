package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SelectedAlternative03Test {

    private static final String ALT2 = "alt2";

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative03test.testmultiplealternativebeanswithproducers";

    @Test
    void testMultipleAlternativeBeansWithProducers() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative03test.testmultiplealternativebeanswithproducers.Delta delta =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative03test.testmultiplealternativebeanswithproducers.Delta.class);
            assertNotNull(delta);
            assertEquals(ALT2, delta.ping());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
