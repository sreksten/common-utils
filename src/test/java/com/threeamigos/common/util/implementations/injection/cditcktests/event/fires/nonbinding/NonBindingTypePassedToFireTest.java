package com.threeamigos.common.util.implementations.injection.cditcktests.event.fires.nonbinding;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NonBindingTypePassedToFireTest {

    @Test
    void testExceptionThrownIfNonBindingTypePassedToFire() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                OwlFinch_Broken.class,
                AnimalStereotype.class,
                AnimalStereotypeAnnotationLiteral.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            OwlFinch_Broken bean = syringe.getBeanManager().createInstance().select(OwlFinch_Broken.class).get();
            assertThrows(IllegalArgumentException.class, bean::methodThatFiresEvent);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class OwlFinch_Broken {
        @Inject
        @Any
        private Event<String> simpleEvent;

        void methodThatFiresEvent() {
            simpleEvent.select(new AnimalStereotypeAnnotationLiteral()).fire("An event");
        }
    }

    @Stereotype
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    @RequestScoped
    public @interface AnimalStereotype {
    }

    static class AnimalStereotypeAnnotationLiteral extends AnnotationLiteral<AnimalStereotype>
            implements AnimalStereotype {
        private static final long serialVersionUID = 1L;
    }
}
