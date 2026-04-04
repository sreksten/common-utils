package com.threeamigos.common.util.implementations.injection.cditcktests.event.metadata.injectionpoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMetadataInjectionPointTest {

    @Test
    void testGetBean() {
        Syringe syringe = newSyringe();
        try {
            Notifier notifier = contextualReference(syringe, Notifier.class);
            InfoObserver infoObserver = contextualReference(syringe, InfoObserver.class);

            notifier.fireInfoEvent();
            Bean<?> lastBean = infoObserver.getLastBean();
            assertNotNull(lastBean);
            assertEquals(Notifier.class, lastBean.getBeanClass());

            notifier.fireInitializerInfoEvent();
            lastBean = infoObserver.getLastBean();
            assertNotNull(lastBean);
            assertEquals(Notifier.class, lastBean.getBeanClass());

            notifier.fireConstructorInfoEvent();
            lastBean = infoObserver.getLastBean();
            assertNotNull(lastBean);
            assertEquals(Notifier.class, lastBean.getBeanClass());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIsTransient() {
        Syringe syringe = newSyringe();
        try {
            Notifier notifier = contextualReference(syringe, Notifier.class);
            InfoObserver infoObserver = contextualReference(syringe, InfoObserver.class);

            notifier.fireInfoEvent();
            assertFalse(infoObserver.isLastIsTransient());

            notifier.fireTransientInfoEvent();
            assertTrue(infoObserver.isLastIsTransient());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetType() {
        Syringe syringe = newSyringe();
        try {
            Notifier notifier = contextualReference(syringe, Notifier.class);
            InfoObserver infoObserver = contextualReference(syringe, InfoObserver.class);
            Type eventInfoLiteralType = new TypeLiteral<Event<Info>>() {
            }.getType();

            notifier.fireInfoEvent();
            Type lastType = infoObserver.getLastType();
            assertNotNull(lastType);
            assertEquals(eventInfoLiteralType, lastType);

            notifier.fireInitializerInfoEvent();
            lastType = infoObserver.getLastType();
            assertNotNull(lastType);
            assertEquals(eventInfoLiteralType, lastType);

            notifier.fireConstructorInfoEvent();
            lastType = infoObserver.getLastType();
            assertNotNull(lastType);
            assertEquals(eventInfoLiteralType, lastType);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetQualifiers() {
        Syringe syringe = newSyringe();
        try {
            Notifier notifier = contextualReference(syringe, Notifier.class);
            InfoObserver infoObserver = contextualReference(syringe, InfoObserver.class);

            notifier.fireInfoEvent();
            Set<Annotation> lastQualifiers = infoObserver.getLastQualifiers();
            assertNotNull(lastQualifiers);
            assertAnnotationSetMatches(lastQualifiers, Default.class);

            notifier.fireConstructorInfoEvent();
            lastQualifiers = infoObserver.getLastQualifiers();
            assertNotNull(lastQualifiers);
            assertAnnotationSetMatches(lastQualifiers, Nice.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetMember() {
        Syringe syringe = newSyringe();
        try {
            Notifier notifier = contextualReference(syringe, Notifier.class);
            InfoObserver infoObserver = contextualReference(syringe, InfoObserver.class);

            notifier.fireInfoEvent();
            Member lastMember = infoObserver.getLastMember();
            assertNotNull(lastMember);
            assertTrue(lastMember instanceof Field);
            Field field = (Field) lastMember;
            assertEquals("infoEvent", field.getName());
            assertEquals(Event.class, field.getType());
            assertEquals(Notifier.class, field.getDeclaringClass());

            notifier.fireInitializerInfoEvent();
            lastMember = infoObserver.getLastMember();
            assertNotNull(lastMember);
            assertTrue(lastMember instanceof Method);
            Method method = (Method) lastMember;
            assertEquals("setInitializerInjectionInfoEvent", method.getName());
            assertEquals(1, method.getParameterTypes().length);
            assertEquals(Notifier.class, method.getDeclaringClass());

            notifier.fireConstructorInfoEvent();
            lastMember = infoObserver.getLastMember();
            assertNotNull(lastMember);
            assertTrue(lastMember instanceof Constructor);
            Constructor<?> constructor = (Constructor<?>) lastMember;
            assertEquals(1, constructor.getParameterTypes().length);
            assertEquals(Notifier.class, constructor.getDeclaringClass());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetAnnotatedType() {
        Syringe syringe = newSyringe();
        try {
            Notifier notifier = contextualReference(syringe, Notifier.class);
            InfoObserver infoObserver = contextualReference(syringe, InfoObserver.class);

            notifier.fireInfoEvent();
            Annotated lastAnnotated = infoObserver.getLastAnnotated();
            assertTrue(lastAnnotated instanceof AnnotatedField);
            assertEquals("infoEvent", ((AnnotatedField<?>) lastAnnotated).getJavaMember().getName());
            assertTrue(lastAnnotated.isAnnotationPresent(Inject.class));

            notifier.fireInitializerInfoEvent();
            lastAnnotated = infoObserver.getLastAnnotated();
            assertTrue(lastAnnotated instanceof AnnotatedParameter);
            assertEquals(0, ((AnnotatedParameter<?>) lastAnnotated).getPosition());

            notifier.fireConstructorInfoEvent();
            lastAnnotated = infoObserver.getLastAnnotated();
            assertTrue(lastAnnotated instanceof AnnotatedParameter);
            assertEquals(0, ((AnnotatedParameter<?>) lastAnnotated).getPosition());
            assertTrue(lastAnnotated.isAnnotationPresent(Nice.class));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Notifier.class, InfoObserver.class, Info.class, Nice.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        ((BeanManagerImpl) syringe.getBeanManager()).getContextManager().activateRequest();
        return syringe;
    }

    private <T> T contextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }

    private void assertAnnotationSetMatches(Set<Annotation> annotations, Class<? extends Annotation>... expectedTypes) {
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

    @RequestScoped
    static class Notifier {
        @Inject
        InfoObserver infoObserver;

        @Inject
        private Event<Info> infoEvent;

        @Inject
        private transient Event<Info> transientInfoEvent;

        private Event<Info> constructorInjectionInfoEvent;
        private Event<Info> initializerInjectionInfoEvent;

        public Notifier() {
        }

        @Inject
        public Notifier(@Nice Event<Info> constructorInjectionInfoEvent) {
            this.constructorInjectionInfoEvent = constructorInjectionInfoEvent;
        }

        @Inject
        void setInitializerInjectionInfoEvent(Event<Info> initializerInjectionInfoEvent) {
            this.initializerInjectionInfoEvent = initializerInjectionInfoEvent;
        }

        void fireInfoEvent() {
            infoObserver.reset();
            infoEvent.fire(new Info());
        }

        void fireTransientInfoEvent() {
            infoObserver.reset();
            transientInfoEvent.fire(new Info());
        }

        void fireConstructorInfoEvent() {
            infoObserver.reset();
            constructorInjectionInfoEvent.fire(new Info());
        }

        void fireInitializerInfoEvent() {
            infoObserver.reset();
            initializerInjectionInfoEvent.fire(new Info());
        }
    }

    @RequestScoped
    static class InfoObserver {
        private Bean<?> lastBean;
        private boolean lastIsTransient;
        private Type lastType;
        private Set<Annotation> lastQualifiers;
        private Member lastMember;
        private Annotated lastAnnotated;

        void observeInfo(@Observes @Any Info info, EventMetadata metadata) {
            InjectionPoint injectionPoint = metadata.getInjectionPoint();
            lastBean = injectionPoint.getBean();
            lastIsTransient = injectionPoint.isTransient();
            lastType = injectionPoint.getType();
            lastQualifiers = injectionPoint.getQualifiers();
            lastMember = injectionPoint.getMember();
            lastAnnotated = injectionPoint.getAnnotated();
        }

        Bean<?> getLastBean() {
            return lastBean;
        }

        boolean isLastIsTransient() {
            return lastIsTransient;
        }

        Type getLastType() {
            return lastType;
        }

        Set<Annotation> getLastQualifiers() {
            return lastQualifiers;
        }

        Member getLastMember() {
            return lastMember;
        }

        Annotated getLastAnnotated() {
            return lastAnnotated;
        }

        void reset() {
            lastBean = null;
            lastIsTransient = false;
            lastType = null;
            lastQualifiers = null;
            lastMember = null;
            lastAnnotated = null;
        }
    }

    static class Info {
    }

    @Qualifier
    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    public @interface Nice {
    }
}
