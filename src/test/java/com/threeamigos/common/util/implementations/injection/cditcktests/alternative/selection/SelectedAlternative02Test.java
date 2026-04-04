package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SelectedAlternative02Test {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable";

    @Test
    void testDependencyResolvable() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Alpha alpha =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Alpha.class);
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Bravo bravo =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Bravo.class);
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Charlie charlie =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Charlie.class);

            assertNotNull(alpha);
            assertNotNull(bravo);
            assertNotNull(charlie);

            assertEquals(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Bar.class.getName(),
                    alpha.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.TestBean.class).getId());
            assertEquals(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Bar.class.getName(),
                    bravo.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.TestBean.class).getId());
            assertEquals(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.Bar.class.getName(),
                    charlie.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable.TestBean.class).getId());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
