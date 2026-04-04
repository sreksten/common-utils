package com.threeamigos.common.util.implementations.injection.cditcktests.event.bindingTypes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBindingTypesTest {

    @Test
    void testEventBindingTypeTargetsMethodFieldParameterElementTypes() {
        Syringe syringe = newSyringe();
        try {
            Animal animal = new Animal();
            syringe.getBeanManager().getEvent().select(Animal.class, new TameAnnotationLiteral()).fire(animal);
            syringe.getBeanManager().createInstance().select(AnimalAssessment.class).get().classifyAsTame(animal);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventBindingTypeTargetsFieldParameterElementTypes() {
        Syringe syringe = newSyringe();
        try {
            Animal animal = new Animal();
            syringe.getBeanManager().getEvent().select(Animal.class, new WildAnnotationLiteral()).fire(animal);
            syringe.getBeanManager().createInstance().select(AnimalAssessment.class).get().classifyAsWild(animal);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonRuntimeBindingTypeIsNotAnEventBindingType() {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();
            try {
                DiscerningObserver observer = beanManager.createInstance().select(DiscerningObserver.class).get();
                observer.reset();
                EventEmitter emitter = beanManager.createInstance().select(EventEmitter.class).get();

                emitter.fireEvent();
                assertTrue(observer.getNumTimesAnyBindingTypeEventObserved() == 1);
                assertTrue(observer.getNumTimesNonRuntimeBindingTypeObserved() == 1);

                emitter.fireEventWithNonRuntimeBindingType();
                assertTrue(observer.getNumTimesAnyBindingTypeEventObserved() == 2);
                assertTrue(observer.getNumTimesNonRuntimeBindingTypeObserved() == 2);
            } finally {
                beanManager.getContextManager().deactivateRequest();
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testFireEventWithNonRuntimeBindingTypeFails() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> syringe.getBeanManager().getEvent().select(Animal.class, new NonRuntimeBindingType.Literal()).fire(new Animal()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testFireEventWithNonBindingAnnotationsFails() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> syringe.getBeanManager().getEvent().select(Animal.class, new NonBindingType.Literal()).fire(new Animal()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventAlwaysHasAnyBinding() {
        Syringe syringe = newSyringe();
        try {
            Bean<Event<Animal>> animalEventBean = getUniqueBean(syringe, new TypeLiteral<Event<Animal>>() {
            }, new WildAnnotationLiteral());
            assertTrue(animalEventBean.getQualifiers().contains(Any.Literal.INSTANCE));

            Bean<Event<Animal>> tameAnimalEventBean = getUniqueBean(syringe, new TypeLiteral<Event<Animal>>() {
            }, new TameAnnotationLiteral());
            assertTrue(tameAnimalEventBean.getQualifiers().contains(Any.Literal.INSTANCE));

            Bean<Event<Animal>> wildAnimalEventBean = getUniqueBean(syringe, new TypeLiteral<Event<Animal>>() {
            }, new WildAnnotationLiteral());
            assertTrue(wildAnimalEventBean.getQualifiers().contains(Any.Literal.INSTANCE));
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Syringe syringe, TypeLiteral<T> type, java.lang.annotation.Annotation... qualifiers) {
        java.util.Set<Bean<T>> beans = (java.util.Set) syringe.getBeanManager().getBeans(type.getType(), qualifiers);
        return (Bean<T>) syringe.getBeanManager().resolve((java.util.Set) beans);
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Animal.class,
                AnimalAssessment.class,
                DiscerningObserver.class,
                EventEmitter.class,
                Extra.class,
                NonBindingType.class,
                NonRuntimeBindingType.class,
                Tame.class,
                Wild.class,
                TameAnnotationLiteral.class,
                WildAnnotationLiteral.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    public static class Animal {
    }

    @Dependent
    public static class AnimalAssessment {
        @Inject
        @Any
        Event<Animal> animalEvent;

        @Inject
        @Tame
        Event<Animal> tameAnimalEvent;

        @Inject
        @Any
        @Wild
        Event<Animal> wildAnimalEvent;

        public void classifyAsTame(Animal animal) {
            tameAnimalEvent.fire(animal);
        }

        public void classifyAsWild(Animal animal) {
            wildAnimalEvent.fire(animal);
        }

        public void assess(Animal animal) {
            animalEvent.fire(animal);
        }
    }

    @RequestScoped
    public static class DiscerningObserver {
        private int numTimesAnyBindingTypeEventObserved;
        private int numTimesNonRuntimeBindingTypeObserved;

        public void observeAny(@Observes @Extra String event) {
            numTimesAnyBindingTypeEventObserved++;
        }

        public void observeNonRuntime(@Observes @Extra @NonRuntimeBindingType String event) {
            numTimesNonRuntimeBindingTypeObserved++;
        }

        public int getNumTimesAnyBindingTypeEventObserved() {
            return numTimesAnyBindingTypeEventObserved;
        }

        public int getNumTimesNonRuntimeBindingTypeObserved() {
            return numTimesNonRuntimeBindingTypeObserved;
        }

        public void reset() {
            numTimesAnyBindingTypeEventObserved = 0;
            numTimesNonRuntimeBindingTypeObserved = 0;
        }
    }

    @Dependent
    public static class EventEmitter {
        @Inject
        @Extra
        Event<String> stringEvent;

        @Inject
        @Extra
        @NonRuntimeBindingType
        Event<String> stringEventWithAnyAndNonRuntimeBindingType;

        @Inject
        @NonRuntimeBindingType
        Event<String> stringEventWithOnlyNonRuntimeBindingType;

        public void fireEvent() {
            stringEvent.fire("event");
        }

        public void fireEventWithNonRuntimeBindingType() {
            stringEventWithAnyAndNonRuntimeBindingType.fire("event");
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier
    public @interface Extra {
    }

    @Target({FIELD, PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NonBindingType {
        class Literal extends AnnotationLiteral<NonBindingType> implements NonBindingType {
        }
    }

    @Target({FIELD, PARAMETER})
    @Qualifier
    @Retention(RetentionPolicy.CLASS)
    public @interface NonRuntimeBindingType {
        class Literal extends AnnotationLiteral<NonRuntimeBindingType> implements NonRuntimeBindingType {
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier
    public @interface Tame {
    }

    public static class TameAnnotationLiteral extends AnnotationLiteral<Tame> implements Tame {
    }

    @Target({FIELD, PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier
    public @interface Wild {
    }

    public static class WildAnnotationLiteral extends AnnotationLiteral<Wild> implements Wild {
    }
}
