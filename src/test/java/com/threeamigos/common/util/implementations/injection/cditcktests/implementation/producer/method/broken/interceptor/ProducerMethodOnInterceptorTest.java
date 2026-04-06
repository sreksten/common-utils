package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.producer.method.broken.interceptor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProducerMethodOnInterceptorTest {

    @Test
    void testProducerMethodNotAllowedOnInterceptor() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Number.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Secure.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SimpleInterceptor_Broken.class, BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::start);
        } finally {
            syringe.shutdown();
        }
    }
}
