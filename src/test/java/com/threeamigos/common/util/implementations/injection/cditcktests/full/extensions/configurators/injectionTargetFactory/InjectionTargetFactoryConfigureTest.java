package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.injectionTargetFactory;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InjectionTargetFactoryConfigureTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                IoCForFramework.class,
                NotOurClass.class,
                SomeService.class,
                SomeServiceImpl.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionTargetFactoryConfigure() {
        NotOurClass noc = getReference(NotOurClass.class);
        IoCForFramework ioc = getReference(IoCForFramework.class);

        assertNotNull(noc.getService());
        noc.getService().ping();

        assertTrue(ioc.wasExceptionThrown());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T getReference(Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        CreationalContext<T> cc = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, beanClass, cc);
    }
}
