package com.threeamigos.common.util.implementations.injection.cditcktests.event.metadata;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMetadataTest {

    @Test
    void testSimpleEvent() {
        Syringe syringe = newSyringe();
        try {
            SimpleEventNotifier notifier = contextualReference(syringe, SimpleEventNotifier.class);
            SimpleEventObserver observer = contextualReference(syringe, SimpleEventObserver.class);
            assertNotNull(notifier);
            assertNotNull(observer);

            notifier.fireSimpleEvent();
            verifyMetadata(observer.getSyncMetadata(), true, SimpleEvent.class, Any.class);

            notifier.fireSimpleEventWithQualifiers();
            verifyMetadata(observer.getSyncMetadata(), true, SimpleEvent.class, Alpha.class, Bravo.class, Any.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testParameterizedResolvedType() {
        Syringe syringe = newSyringe();
        try {
            DuckNotifier notifier = contextualReference(syringe, DuckNotifier.class);
            DuckObserver observer = contextualReference(syringe, DuckObserver.class);
            assertNotNull(notifier);
            assertNotNull(observer);

            notifier.fireStringDuck();
            verifyMetadata(observer.getMetadata(), true, new TypeLiteral<Duck<String>>() {
            }.getType(), Any.class);

            notifier.fireMapDuck();
            verifyMetadata(observer.getMetadata(), true, new TypeLiteral<Duck<Map<String, Integer>>>() {
            }.getType(), Any.class, Bravo.class);

            notifier.fireListDuck();
            verifyMetadata(observer.getMetadata(), true, new TypeLiteral<ArrayList<Duck<Number>>>() {
            }.getType(), Any.class);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                SimpleEventNotifier.class, SimpleEventObserver.class,
                DuckNotifier.class, DuckObserver.class,
                SimpleEvent.class, Duck.class,
                Alpha.class, Bravo.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private <T> T contextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }

    private void verifyMetadata(EventMetadata metadata, boolean injectionPointAvailable, Type resolvedType,
                                Class<? extends Annotation>... qualifiers) {
        assertNotNull(metadata);
        if (injectionPointAvailable) {
            assertNotNull(metadata.getInjectionPoint());
        } else {
            assertNull(metadata.getInjectionPoint());
        }
        assertEquals(resolvedType, metadata.getType());
        assertAnnotationSetMatches(metadata.getQualifiers(), qualifiers);
    }

    private void assertAnnotationSetMatches(Set<Annotation> annotations, Class<? extends Annotation>... expectedTypes) {
        assertNotNull(annotations);
        assertEquals(expectedTypes.length, annotations.size());
        for (Class<? extends Annotation> expectedType : expectedTypes) {
            boolean found = false;
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(expectedType)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Missing qualifier: " + expectedType.getName());
        }
    }

    @Dependent
    static class SimpleEventNotifier {
        @Inject
        SimpleEventObserver observer;

        @Inject
        @Any
        Event<SimpleEvent> event;

        @Inject
        @Alpha
        Event<SimpleEvent> alphaEvent;

        void fireSimpleEvent() {
            observer.reset();
            event.fire(new SimpleEvent());
        }

        void fireSimpleEventWithQualifiers() {
            observer.reset();
            alphaEvent.select(BravoLiteral.INSTANCE).fire(new SimpleEvent());
        }
    }

    @ApplicationScoped
    static class SimpleEventObserver {
        private EventMetadata syncMetadata;

        void observeSimpleEvent(@Observes SimpleEvent event, EventMetadata metadata) {
            this.syncMetadata = metadata;
        }

        EventMetadata getSyncMetadata() {
            return syncMetadata;
        }

        void reset() {
            syncMetadata = null;
        }
    }

    @Dependent
    static class DuckNotifier {
        @Inject
        DuckObserver observer;

        @Inject
        @Any
        Event<Duck<String>> event;

        @Inject
        @Bravo
        Event<Duck<Map<String, Integer>>> mapDuckEvent;

        @Inject
        @Any
        Event<List<Duck<Number>>> listDuckEvent;

        void fireStringDuck() {
            observer.reset();
            event.fire(new Duck<String>());
        }

        void fireMapDuck() {
            observer.reset();
            mapDuckEvent.fire(new Duck<Map<String, Integer>>());
        }

        void fireListDuck() {
            observer.reset();
            listDuckEvent.fire(new ArrayList<Duck<Number>>());
        }
    }

    @ApplicationScoped
    static class DuckObserver {
        private EventMetadata metadata;

        void observeDuck(@Observes Duck<?> event, EventMetadata metadata) {
            this.metadata = metadata;
        }

        void observeDuck(@Observes List<Duck<?>> event, EventMetadata metadata) {
            this.metadata = metadata;
        }

        EventMetadata getMetadata() {
            return metadata;
        }

        void reset() {
            metadata = null;
        }
    }

    static class SimpleEvent {
    }

    static class Duck<T> {
    }

    @Qualifier
    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    public @interface Alpha {
    }

    @Qualifier
    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    public @interface Bravo {
    }

    static class BravoLiteral extends AnnotationLiteral<Bravo> implements Bravo {
        private static final long serialVersionUID = 1L;
        private static final Bravo INSTANCE = new BravoLiteral();
    }
}
