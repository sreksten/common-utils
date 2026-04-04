package com.threeamigos.common.util.implementations.injection.cditcktests.event.resolve.binding;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DuplicateBindingTypesWhenResolvingTest {

    @Test
    void testDuplicateBindingTypesWhenResolvingFails() {
        Syringe syringe = newSyringe(AnObserver.class);
        try {
            assertThrows(IllegalArgumentException.class, () -> syringe.getBeanManager().resolveObserverMethods(
                    new AnEventType(),
                    new BindingTypeABinding("a1"),
                    new BindingTypeABinding("a2")));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe(Class<?>... discoveredClasses) {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        for (Class<?> discoveredClass : discoveredClasses) {
            syringe.addDiscoveredClass(discoveredClass, BeanArchiveMode.EXPLICIT);
        }
        syringe.start();
        return syringe;
    }

    static class AnEventType {
    }

    @Dependent
    static class AnObserver {
        boolean wasNotified = false;

        void observer(@Observes AnEventType event) {
            wasNotified = true;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    @interface BindingTypeA {
        String value() default "";
    }

    static class BindingTypeABinding extends AnnotationLiteral<BindingTypeA> implements BindingTypeA {
        private static final long serialVersionUID = 1L;

        private final String value;

        BindingTypeABinding(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
