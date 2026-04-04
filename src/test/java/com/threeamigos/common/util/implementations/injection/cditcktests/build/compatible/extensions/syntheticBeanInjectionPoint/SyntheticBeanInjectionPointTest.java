package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint.syntheticbeaninjectionpointtest.test.MyApplicationScopedBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint.syntheticbeaninjectionpointtest.test.MyApplicationScopedBeanCreator;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint.syntheticbeaninjectionpointtest.test.MyDependentBean;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint.syntheticbeaninjectionpointtest.test.MyDependentBeanCreator;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint.syntheticbeaninjectionpointtest.test.MyDependentBeanDisposer;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint.syntheticbeaninjectionpointtest.test.SyntheticBeanInjectionPointExtension;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

class SyntheticBeanInjectionPointTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticBeanInjectionPoint.syntheticbeaninjectionpointtest.test";

    @Test
    void test() {
        MyDependentBeanCreator.reset();
        MyDependentBeanDisposer.reset();
        MyApplicationScopedBeanCreator.reset();

        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.IMPLICIT);
        syringe.addBuildCompatibleExtension(SyntheticBeanInjectionPointExtension.class.getName());
        syringe.setup();
        try {
            Instance<Object> lookup = syringe.getBeanManager().createInstance();

            Instance.Handle<MyDependentBean> handle = lookup.select(MyDependentBean.class).getHandle();
            try {
                handle.get();
            } catch (Exception e) {
                fail(e);
            }
            assertNotNull(MyDependentBeanCreator.getLookedUp());

            try {
                handle.destroy();
            } catch (Exception ignored) {
            }
            assertNull(MyDependentBeanDisposer.getLookedUp());

            try {
                lookup.select(MyApplicationScopedBean.class).get();
            } catch (Exception ignored) {
            }
            assertNull(MyApplicationScopedBeanCreator.getLookedUp());
        } finally {
            syringe.shutdown();
        }
    }
}
