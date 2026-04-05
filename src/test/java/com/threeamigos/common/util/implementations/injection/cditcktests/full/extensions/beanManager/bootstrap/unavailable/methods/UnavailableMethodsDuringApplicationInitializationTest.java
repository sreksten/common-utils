package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.bootstrap.unavailable.methods;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UnavailableMethodsDuringApplicationInitializationTest {

    private Syringe syringe;

    @AfterEach
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testUnavailableMethods() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Foo.class,
                Transactional.class,
                WrongExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(WrongExtension.class.getName());

        assertDoesNotThrow(syringe::setup);
    }
}
