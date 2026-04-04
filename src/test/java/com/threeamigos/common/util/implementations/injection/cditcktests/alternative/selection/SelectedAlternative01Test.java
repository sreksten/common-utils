package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.selection.Tame;
import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.selection.Wild;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

class SelectedAlternative01Test {

    private static final String FIXTURE_MANAGED_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected";
    private static final String FIXTURE_PRODUCER_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected";

    @Test
    void testAlternativeManagedBeanSelected() {
        Syringe syringe = new Syringe(FIXTURE_MANAGED_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Alpha alpha =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Alpha.class);
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Bravo bravo =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Bravo.class);
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Charlie charlie =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Charlie.class);

            alpha.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Foo.class);
            bravo.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Foo.class);
            charlie.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativemanagedbeanselected.Foo.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAlternativeProducerSelected() {
        Syringe syringe = new Syringe(FIXTURE_PRODUCER_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Alpha alpha =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Alpha.class);
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bravo bravo =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bravo.class);
            com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Charlie charlie =
                    resolveReference(beanManager, com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Charlie.class);

            alpha.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bar.class, Wild.Literal.INSTANCE);
            bravo.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bar.class, Wild.Literal.INSTANCE);
            charlie.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bar.class, Wild.Literal.INSTANCE);

            alpha.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bar.class, Tame.Literal.INSTANCE);
            bravo.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bar.class, Tame.Literal.INSTANCE);
            charlie.assertAvailable(com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected.Bar.class, Tame.Literal.INSTANCE);
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
