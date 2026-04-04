package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.notdiscovered.NotDiscoveredBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.test.CustomStereotypeExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.test.MyService;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomStereotypeTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customStereotype.customstereotypetest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(CustomStereotypeExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            Set<Bean<?>> serviceBeans = beanManager.getBeans(MyService.class);
            assertEquals(1, serviceBeans.size());
            assertEquals(ApplicationScoped.class, serviceBeans.iterator().next().getScope());
            assertEquals("Hello!", resolveReference(beanManager, MyService.class).hello());

            assertTrue(beanManager.getBeans(NotDiscoveredBean.class).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
