package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType.syntheticobserverofparameterizedtypetest.test.MyObserver;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType.syntheticobserverofparameterizedtypetest.test.MyService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType.syntheticobserverofparameterizedtypetest.test.SyntheticObserverOfParameterizedTypeExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntheticObserverOfParameterizedTypeTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType.syntheticobserverofparameterizedtypetest.test";

    @Test
    void test() {
        MyObserver.reset();

        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(SyntheticObserverOfParameterizedTypeExtension.class.getName());
        syringe.setup();
        try {
            MyService myService = syringe.getBeanManager().createInstance().select(MyService.class).get();
            myService.fireEvent();

            List<String> expected = Arrays.asList("Hello World", "Hello again");
            assertEquals(expected, MyObserver.getObserved());
        } finally {
            syringe.shutdown();
        }
    }
}
