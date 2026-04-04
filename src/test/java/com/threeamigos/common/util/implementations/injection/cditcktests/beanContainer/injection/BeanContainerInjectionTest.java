package com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.injection;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.injection.beancontainerinjectiontest.testinjectionofbeancontainertype.Probe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanContainerInjectionTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.injection.beancontainerinjectiontest.testinjectionofbeancontainertype";

    @Test
    void testInjectionOfBeanContainerType() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Probe probe = resolveReference(beanManager, Probe.class);

            probe.getMyBean().getBeanContainer().isNormalScope(ApplicationScoped.class);

            Instance<BeanContainer> beanContainerInstance = probe.getInstance().select(BeanContainer.class, Default.Literal.INSTANCE);
            assertTrue(beanContainerInstance.isResolvable());
            assertEquals(Dependent.class, beanContainerInstance.getHandle().getBean().getScope());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
