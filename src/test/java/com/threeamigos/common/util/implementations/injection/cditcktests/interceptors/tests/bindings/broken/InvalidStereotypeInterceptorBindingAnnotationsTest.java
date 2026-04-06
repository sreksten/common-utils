package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.bindings.broken;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InvalidStereotypeInterceptorBindingAnnotationsTest {

    @Test
    void testInterceptorBindingsWithConflictingAnnotationMembersNotOk() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bar.class,
                FooBinding.class,
                BarBinding.class,
                BazBinding.class,
                FooInterceptor.class,
                BarInterceptor.class,
                YesBazInterceptor.class,
                NoBazInterceptor.class,
                FooStereotype.class,
                BarStereotype.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }
}
