package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverNotificationTest {

    @Test
    void testObserversNotified() {
        Syringe syringe = newSyringe();
        try {
            AnEventType anEvent = new AnEventType();

            resetObservers();
            syringe.getBeanManager().getEvent().select(AnEventType.class, new RoleLiteral("Admin", "hurray")).fire(anEvent);
            assertTrue(AnObserver.wasNotified);
            assertTrue(AnotherObserver.wasNotified);
            assertTrue(LastObserver.wasNotified);
            assertFalse(DisabledObserver.wasNotified);

            resetObservers();
            syringe.getBeanManager().getEvent().select(AnEventType.class).fire(anEvent);
            assertTrue(AnObserver.wasNotified);
            assertFalse(AnotherObserver.wasNotified);
            assertTrue(LastObserver.wasNotified);
            assertFalse(DisabledObserver.wasNotified);

            resetObservers();
            syringe.getBeanManager().getEvent().select(AnEventType.class, new RoleLiteral("user", "hurray")).fire(anEvent);
            assertTrue(AnObserver.wasNotified);
            assertFalse(AnotherObserver.wasNotified);
            assertTrue(LastObserver.wasNotified);
            assertFalse(DisabledObserver.wasNotified);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodNotInvokedIfNoActiveContext() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
        try {
            resetObservers();
            beanManager.getContextManager().deactivateRequest();
            try {
                syringe.getBeanManager().getEvent()
                        .select(AnEventType.class, new RoleLiteral("Admin", "hurray"))
                        .fire(new AnEventType());
                assertFalse(AnotherObserver.wasNotified);
            } finally {
                beanManager.getContextManager().activateRequest();
            }
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                AnEventType.class, AnObserver.class, AnotherObserver.class,
                LastObserver.class, DisabledObserver.class, Role.class, RoleLiteral.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        ((BeanManagerImpl) syringe.getBeanManager()).getContextManager().activateRequest();
        return syringe;
    }

    private void resetObservers() {
        AnObserver.wasNotified = false;
        AnotherObserver.wasNotified = false;
        LastObserver.wasNotified = false;
        DisabledObserver.wasNotified = false;
    }

    static class AnEventType {
    }

    @Dependent
    static class AnObserver {
        static boolean wasNotified;

        void observer(@Observes @Any AnEventType event) {
            wasNotified = true;
        }
    }

    @RequestScoped
    static class AnotherObserver {
        static boolean wasNotified;

        void observer(@Observes @Role("Admin") AnEventType event) {
            wasNotified = true;
        }
    }

    @Dependent
    static class LastObserver {
        static boolean wasNotified;

        void observer(@Observes AnEventType event) {
            wasNotified = true;
        }
    }

    @Alternative
    @Dependent
    static class DisabledObserver {
        static boolean wasNotified;

        void observer(@Observes AnEventType event) {
            wasNotified = true;
        }
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({FIELD, PARAMETER, METHOD, TYPE})
    public @interface Role {
        String value();

        @Nonbinding
        String nonbindingValue() default "blabla";
    }

    static class RoleLiteral extends AnnotationLiteral<Role> implements Role {
        private static final long serialVersionUID = 1L;

        private final String value;
        private final String nonbindingValue;

        RoleLiteral(String value, String nonbindingValue) {
            this.value = value;
            this.nonbindingValue = nonbindingValue;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String nonbindingValue() {
            return nonbindingValue;
        }
    }
}
