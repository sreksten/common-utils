package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customQualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customQualifier.customqualifiertest.test.CustomQualifierExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customQualifier.customqualifiertest.test.MyCustomQualifier;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customQualifier.customqualifiertest.test.MyService;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomQualifierTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.customQualifier.customqualifiertest.test";

    @Test
    void test() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(CustomQualifierExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<?> bean = beanManager.resolve(beanManager.getBeans(MyService.class,
                    new MyCustomQualifier.Literal("something")));
            MyService reference = MyService.class.cast(beanManager.getReference(bean, MyService.class,
                    beanManager.createCreationalContext(bean)));
            assertEquals("bar", reference.hello());
        } finally {
            syringe.shutdown();
        }
    }
}
