package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution.Baz;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution.Foo;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution.Qux;
import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.TrueLiteral;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QualifierNotDeclaredTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotdeclaredtest.testresolution";

    @Test
    void testResolution() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            Bean<?> bean = beanManager.resolve(beanManager.getBeans(Foo.class, TrueLiteral.INSTANCE));
            assertEquals(Baz.class, bean.getBeanClass());

            Qux qux = resolveReference(beanManager, Qux.class);
            assertNotNull(qux);
            assertEquals(1, qux.getFoo().ping());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
