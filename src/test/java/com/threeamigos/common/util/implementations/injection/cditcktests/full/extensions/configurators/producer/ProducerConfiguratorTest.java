package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.producer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProducerConfiguratorTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        ProducerConfiguringExtension.producerCalled.set(false);
        ProducerConfiguringExtension.disposerCalled.set(false);

        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bar.class,
                MassProducer.class,
                ParameterInjectedBean.class,
                ProducerConfiguringExtension.class,
                Some.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProducerConfiguringExtension.class.getName());
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
    void configuratorOptionsTest() {
        Instance<Bar> barInstance = beanManager.createInstance().select(Bar.class, Some.SomeLiteral.INSTANCE);

        assertTrue(barInstance.isResolvable());
        Bar actualInstance = barInstance.get();
        assertTrue(ProducerConfiguringExtension.producerCalled.get());
        assertNull(actualInstance.getParamInjectedBean().getAnnotation());
        barInstance.destroy(actualInstance);
        assertTrue(ProducerConfiguringExtension.disposerCalled.get());
    }
}
