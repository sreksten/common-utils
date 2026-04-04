package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.ChangeBeanQualifierExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyOtherService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test.MyServiceBar;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeBeanQualifierTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeBeanQualifier.changebeanqualifiertest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(ChangeBeanQualifierExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            MyOtherService bean = resolveReference(beanManager, MyOtherService.class);
            assertTrue(bean.getMyService() instanceof MyServiceBar);
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
