package com.threeamigos.common.util.implementations.injection.cditcktests.event.select;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelectEventTest {

    @Test
    void testEventSelectReturnsEventOfSameType() {
        Syringe syringe = newSyringe();
        try {
            AlarmSystem alarm = getContextualReference(syringe, AlarmSystem.class);
            alarm.reset();
            SecuritySensor sensor = getContextualReference(syringe, SecuritySensor.class);

            sensor.securityEvent.fire(new SecurityEvent());
            assertEquals(1, alarm.getNumSecurityEvents());
            assertEquals(0, alarm.getNumSystemTests());
            assertEquals(0, alarm.getNumBreakIns());
            assertEquals(0, alarm.getNumViolentBreakIns());

            sensor.securityEvent.select(new SystemTest.SystemTestLiteral("")).fire(new SecurityEvent());
            assertEquals(2, alarm.getNumSecurityEvents());
            assertEquals(1, alarm.getNumSystemTests());
            assertEquals(0, alarm.getNumBreakIns());
            assertEquals(0, alarm.getNumViolentBreakIns());

            sensor.securityEvent.select(BreakInEvent.class).fire(new BreakInEvent());
            assertEquals(3, alarm.getNumSecurityEvents());
            assertEquals(1, alarm.getNumSystemTests());
            assertEquals(1, alarm.getNumBreakIns());
            assertEquals(0, alarm.getNumViolentBreakIns());

            sensor.securityEvent.select(BreakInEvent.class, new Violent.Literal()).fire(new BreakInEvent());
            assertEquals(4, alarm.getNumSecurityEvents());
            assertEquals(1, alarm.getNumSystemTests());
            assertEquals(2, alarm.getNumBreakIns());
            assertEquals(1, alarm.getNumViolentBreakIns());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventSelectThrowsExceptionIfEventTypeHasTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            SecuritySensor sensor = getContextualReference(syringe, SecuritySensor.class);
            assertThrows(IllegalArgumentException.class, () -> selectIllegalEventType(sensor));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventSelectThrowsExceptionForDuplicateBindingType() {
        Syringe syringe = newSyringe();
        try {
            SecuritySensor sensor = getContextualReference(syringe, SecuritySensor.class);
            assertThrows(IllegalArgumentException.class, () -> sensor.securityEvent.select(
                    new SystemTest.SystemTestLiteral("a"),
                    new SystemTest.SystemTestLiteral("b")));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventSelectWithSubtypeThrowsExceptionForDuplicateBindingType() {
        Syringe syringe = newSyringe();
        try {
            SecuritySensor sensor = getContextualReference(syringe, SecuritySensor.class);
            assertThrows(IllegalArgumentException.class, () -> sensor.securityEvent.select(
                    BreakInEvent.class,
                    new SystemTest.SystemTestLiteral("a"),
                    new SystemTest.SystemTestLiteral("b")));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventSelectThrowsExceptionIfAnnotationIsNotBindingType() {
        Syringe syringe = newSyringe();
        try {
            SecuritySensor sensor = getContextualReference(syringe, SecuritySensor.class);
            assertThrows(IllegalArgumentException.class, () -> sensor.securityEvent.select(new NotABindingType.Literal()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventSelectWithSubtypeThrowsExceptionIfAnnotationIsNotBindingType() {
        Syringe syringe = newSyringe();
        try {
            SecuritySensor sensor = getContextualReference(syringe, SecuritySensor.class);
            assertThrows(IllegalArgumentException.class,
                    () -> sensor.securityEvent.select(BreakInEvent.class, new NotABindingType.Literal()));
        } finally {
            syringe.shutdown();
        }
    }

    private <T> void selectIllegalEventType(SecuritySensor sensor) {
        sensor.securityEvent.select(new TypeLiteral<SecurityEvent_Illegal<T>>() {
        });
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(SecurityEvent.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BreakInEvent.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SecurityEvent_Illegal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SystemTest.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Violent.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NotABindingType.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AlarmSystem.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SecuritySensor.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        syringe.activateRequestContextIfNeeded();
        return syringe;
    }

    private static <T> T getContextualReference(Syringe syringe, Class<T> type) {
        jakarta.enterprise.inject.spi.BeanManager beanManager = syringe.getBeanManager();
        jakarta.enterprise.inject.spi.Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    @RequestScoped
    static class AlarmSystem {
        private int numSecurityEvents = 0;
        private int numSystemTests = 0;
        private int numBreakIns = 0;
        private int numViolentBreakIns = 0;

        void securityEventOccurred(@Observes @Any SecurityEvent event) {
            numSecurityEvents++;
        }

        void selfTest(@Observes @SystemTest SecurityEvent event) {
            numSystemTests++;
        }

        void breakInOccurred(@Observes @Any BreakInEvent event) {
            numBreakIns++;
        }

        void securityBreeched(@Observes @Violent BreakInEvent event) {
            numViolentBreakIns++;
        }

        int getNumSystemTests() {
            return numSystemTests;
        }

        int getNumSecurityEvents() {
            return numSecurityEvents;
        }

        int getNumBreakIns() {
            return numBreakIns;
        }

        int getNumViolentBreakIns() {
            return numViolentBreakIns;
        }

        void reset() {
            numBreakIns = 0;
            numViolentBreakIns = 0;
            numSecurityEvents = 0;
            numSystemTests = 0;
        }
    }

    static class SecurityEvent {
    }

    static class BreakInEvent extends SecurityEvent {
    }

    static class SecurityEvent_Illegal<T> extends SecurityEvent {
    }

    @Dependent
    static class SecuritySensor {
        @Inject
        @Any
        Event<SecurityEvent> securityEvent;
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface SystemTest {

        String value() default "";

        class SystemTestLiteral extends AnnotationLiteral<SystemTest> implements SystemTest {
            private static final long serialVersionUID = 1L;

            private final String value;

            SystemTestLiteral(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Violent {
        class Literal extends AnnotationLiteral<Violent> implements Violent {
            private static final long serialVersionUID = 1L;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    public @interface NotABindingType {
        class Literal extends AnnotationLiteral<NotABindingType> implements NotABindingType {
            private static final long serialVersionUID = 1L;
        }
    }
}
