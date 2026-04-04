package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.test.ChangeObserverQualifierExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.test.MyConsumer;
import com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.test.MyProducer;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangeObserverQualifierTest {

    private static final String TEST_FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.test";

    private static final String SCOPE_FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.testscopeisretained";

    @Test
    void test() {
        Syringe syringe = new Syringe(TEST_FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(ChangeObserverQualifierExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            MyConsumer.clearEvents();
            resolveReference(beanManager, MyProducer.class).produce();
            assertEquals(Collections.singleton("qualified"), MyConsumer.getEvents());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testScopeIsRetained() {
        Syringe syringe = new Syringe(SCOPE_FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(
                com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.testscopeisretained.ChangeObserverQualifierExtension.class.getName());
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            Set<Bean<?>> producerBeans = beanManager.getBeans(
                    com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.testscopeisretained.MyProducer.class);
            assertEquals(1, producerBeans.size());
            assertEquals(ApplicationScoped.class, producerBeans.iterator().next().getScope());

            Set<Bean<?>> consumerBeans = beanManager.getBeans(
                    com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.changeObserverQualifier.changeobserverqualifiertest.testscopeisretained.MyConsumer.class);
            assertEquals(1, consumerBeans.size());
            assertEquals(ApplicationScoped.class, consumerBeans.iterator().next().getScope());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
