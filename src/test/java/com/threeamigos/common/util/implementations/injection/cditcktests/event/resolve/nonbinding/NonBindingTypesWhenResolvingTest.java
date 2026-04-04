package com.threeamigos.common.util.implementations.injection.cditcktests.event.resolve.nonbinding;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NonBindingTypesWhenResolvingTest {

    @Test
    void testNonBindingTypeAnnotationWhenResolvingFails() {
        Syringe syringe = newSyringe(AnObserver.class);
        try {
            assertThrows(IllegalArgumentException.class, () -> syringe.getBeanManager().resolveObserverMethods(
                    new AnEventType(), new AnimalStereotypeAnnotationLiteral()));
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

    static class AnObserver {
        boolean wasNotified = false;

        void observer(@Observes AnEventType event) {
            wasNotified = true;
        }
    }

    @Stereotype
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    @RequestScoped
    @interface AnimalStereotype {
    }

    static class AnimalStereotypeAnnotationLiteral extends AnnotationLiteral<AnimalStereotype>
            implements AnimalStereotype {
        private static final long serialVersionUID = 1L;
    }
}
