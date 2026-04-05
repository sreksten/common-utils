package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.interceptionFactory;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InterceptionFactoryTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private RequestContextController requestContextController;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(new InMemoryMessageHandler(), InterceptionFactoryTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanManager = syringe.getBeanManager();
        requestContextController = beanManager.createInstance().select(RequestContextController.class).get();
        requestContextController.activate();
    }

    @AfterAll
    void tearDown() {
        if (requestContextController != null) {
            requestContextController.deactivate();
        }
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void producedInstanceIsIntercepted() {
        ActionSequence.reset();
        Product product = beanManager.createInstance().select(Product.class).get();
        assertEquals(4, product.ping());
        ActionSequence.assertSequenceDataEquals(ProductInterceptor1.class, ProductInterceptor2.class,
                ProductInterceptor3.class);
    }

    @Test
    void interceptionFactoryBeanIsAvailable() {
        Bean<?> interceptionFactoryBean = beanManager.resolve(beanManager.getBeans(InterceptionFactory.class));
        assertNotNull(interceptionFactoryBean);
        assertEquals(Dependent.class, interceptionFactoryBean.getScope());

        Set<Annotation> expectedQualifiers =
                Stream.<Annotation>of(Default.Literal.INSTANCE, Any.Literal.INSTANCE).collect(Collectors.toSet());
        assertEquals(expectedQualifiers, interceptionFactoryBean.getQualifiers());
    }

    @Test
    void producedWithFinalMethodIsIntercepted() {
        ActionSequence.reset();
        FinalProduct finalProduct = beanManager.createInstance()
                .select(FinalProduct.class, Custom.CustomLiteral.INSTANCE)
                .get();
        assertEquals(3, finalProduct.ping());
        ActionSequence.assertSequenceDataEquals(ProductInterceptor1.class, ProductInterceptor2.class);
    }
}
