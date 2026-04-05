package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.producer.broken;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ModifyingProducerViaConfigureAndSetTest {

    private Syringe syringe;

    @BeforeAll
    void setUp() {
        AddAndConfigureExtension.configureThanSetExceptionThrown.set(false);
        AddAndConfigureExtension.setThanConfigureExceptionThrown.set(false);

        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AddAndConfigureExtension.class,
                AnotherProducerBean.class,
                Foo.class,
                ProducerBean.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AddAndConfigureExtension.class.getName());
        syringe.setup();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testDeploymentThrowsISE() {
        assertTrue(AddAndConfigureExtension.configureThanSetExceptionThrown.get());
        assertTrue(AddAndConfigureExtension.setThanConfigureExceptionThrown.get());
    }
}
