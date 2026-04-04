package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifierinheritedtest.testresolution.Forest;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifierinheritedtest.testresolution.Larch;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifierinheritedtest.testresolution.Tree;
import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.TrueLiteral;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QualifierInheritedTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifierinheritedtest.testresolution";

    @Test
    void testResolution() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            Bean<?> bean = beanManager.resolve(beanManager.getBeans(Tree.class, TrueLiteral.INSTANCE));
            assertEquals(Larch.class, bean.getBeanClass());

            Forest forest = resolveReference(beanManager, Forest.class);
            assertNotNull(forest);
            assertEquals(0, forest.getTree().ping());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
