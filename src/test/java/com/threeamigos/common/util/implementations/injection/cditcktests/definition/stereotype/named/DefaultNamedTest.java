package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.named;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultNamedTest {

    @Test
    void testStereotypeDeclaringNamed() {
        Syringe syringe = newSyringe();
        BeanManager beanManager = syringe.getBeanManager();
        try {
            Bean<?> fallowBean = beanManager.resolve(beanManager.getBeans(FallowDeer.class));
            assertNotNull(fallowBean);
            assertEquals("fallowDeer", fallowBean.getName());
            assertTrue(fallowBean.getQualifiers().contains(Any.Literal.INSTANCE));
            assertTrue(fallowBean.getQualifiers().contains(Default.Literal.INSTANCE));
            assertEquals(2, fallowBean.getQualifiers().size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStereotypeNamedOverridenByBean() {
        Syringe syringe = newSyringe();
        BeanManager beanManager = syringe.getBeanManager();
        try {
            Bean<?> roeBean = beanManager.resolve(beanManager.getBeans(RoeDeer.class));
            assertNotNull(roeBean);
            assertEquals("roe", roeBean.getName());
            assertTrue(roeBean.getQualifiers().contains(Any.Literal.INSTANCE));
            assertTrue(roeBean.getQualifiers().contains(Default.Literal.INSTANCE));
            assertTrue(roeBean.getQualifiers().contains(NamedLiteral.of("roe")));
            assertEquals(3, roeBean.getQualifiers().size());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                StereotypeWithEmptyNamed.class,
                FallowDeer.class,
                RoeDeer.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @Stereotype
    @Target(TYPE)
    @Retention(RUNTIME)
    @Named
    public @interface StereotypeWithEmptyNamed {
    }

    @StereotypeWithEmptyNamed
    public static class FallowDeer {
    }

    @Named("roe")
    @StereotypeWithEmptyNamed
    public static class RoeDeer {
    }
}
