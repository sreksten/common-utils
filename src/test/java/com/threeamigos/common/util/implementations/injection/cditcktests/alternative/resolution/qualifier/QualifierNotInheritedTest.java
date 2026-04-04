package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotinheritedtest.testresolution.Dungeon;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotinheritedtest.testresolution.Monster;
import com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotinheritedtest.testresolution.Troll;
import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.resolution.qualifier.FalseLiteral;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QualifierNotInheritedTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.alternative.resolution.qualifier.qualifiernotinheritedtest.testresolution";

    @Test
    void testResolution() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            Bean<?> bean = beanManager.resolve(beanManager.getBeans(Monster.class, FalseLiteral.INSTANCE));
            assertEquals(Monster.class, bean.getBeanClass());

            Dungeon dungeon = resolveReference(beanManager, Dungeon.class);
            assertNotNull(dungeon);
            assertEquals(1, dungeon.getMonster().ping());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
