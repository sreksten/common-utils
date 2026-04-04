package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test.MyObserver;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test.MyService;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test.SyntheticObserverExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntheticObserverTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserver.syntheticobservertest.test";

    @Test
    void test() {
        MyObserver.reset();

        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(SyntheticObserverExtension.class.getName());
        syringe.setup();
        try {
            MyService myService = syringe.getBeanManager().createInstance().select(MyService.class).get();
            myService.fireEvent();

            List<String> expected = Arrays.asList(
                    "Hello World with null",
                    "Hello Special with null",
                    "Hello Special with @MyQualifier"
            );
            assertEquals(expected, MyObserver.getObserved());
        } finally {
            syringe.shutdown();
        }
    }
}
