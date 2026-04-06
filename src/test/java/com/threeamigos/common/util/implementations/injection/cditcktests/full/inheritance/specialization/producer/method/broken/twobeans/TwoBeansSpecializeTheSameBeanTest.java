package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.producer.method.broken.twobeans;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TwoBeansSpecializeTheSameBeanTest {

    @Test
    void testTwoBeansSpecializeTheSameBean() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bookshop_Broken.class,
                Expensive.class,
                PictureShop_Broken.class,
                Product.class,
                Shop.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DeploymentException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
