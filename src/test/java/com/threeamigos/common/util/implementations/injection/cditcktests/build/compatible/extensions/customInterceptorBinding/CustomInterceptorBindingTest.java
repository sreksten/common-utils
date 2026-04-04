package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customInterceptorBinding;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customInterceptorBinding.custominterceptorbindingtest.test.CustomInterceptorBindingExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customInterceptorBinding.custominterceptorbindingtest.test.MyService;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomInterceptorBindingTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customInterceptorBinding.custominterceptorbindingtest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(CustomInterceptorBindingExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals("Intercepted: Hello!", resolveReference(beanManager, MyService.class).hello());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
