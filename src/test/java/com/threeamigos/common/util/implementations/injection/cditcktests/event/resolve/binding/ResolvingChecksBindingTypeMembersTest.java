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
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolvingChecksBindingTypeMembersTest {

    @Test
    void testResolvingChecksBindingTypeMembers() {
        Syringe syringe = newSyringe(AnObserver.class, AnotherObserver.class);
        try {
            assertEquals(1, syringe.getBeanManager().resolveObserverMethods(
                    new AnEventType(),
                    new BindingTypeCBinding("first-observer")).size());
            assertEquals(1, syringe.getBeanManager().resolveObserverMethods(
                    new AnEventType(),
                    new BindingTypeCBinding("second-observer")).size());
            assertEquals(0, syringe.getBeanManager().resolveObserverMethods(
                    new AnEventType(),
                    new BindingTypeCBinding("third-observer")).size());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe(Class<?>... discoveredClasses) {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(AnEventType.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BindingTypeC.class, BeanArchiveMode.EXPLICIT);
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

        void observer(@Observes @BindingTypeC("first-observer") AnEventType event) {
            wasNotified = true;
        }
    }

    @Dependent
    static class AnotherObserver {
        boolean wasNotified = false;

        void observer(@Observes @BindingTypeC("second-observer") AnEventType event) {
            wasNotified = true;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface BindingTypeC {
        String value();
    }

    static class BindingTypeCBinding extends AnnotationLiteral<BindingTypeC> implements BindingTypeC {
        private static final long serialVersionUID = 1L;

        private final String value;

        BindingTypeCBinding(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
