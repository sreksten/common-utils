package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.resolve;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class ResolveEventObserversTest {

    private static final String BEAN_MANAGER_RESOLVE_OBSERVERS_METHOD_NAME = "resolveObserverMethods";

    @Test
    void testMultipleObserverMethodsForSameEventPermissible() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(2, syringe.getBeanManager().resolveObserverMethods(new DiskSpaceEvent()).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMultipleObserverMethodsOnBeanPermissible() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, syringe.getBeanManager().resolveObserverMethods(new BatteryEvent()).size());
            assertEquals(2, syringe.getBeanManager().resolveObserverMethods(new DiskSpaceEvent()).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMethodWithParameterAnnotatedWithObservesRegistersObserverMethod() throws Exception {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super Temperature>> temperatureObservers =
                    syringe.getBeanManager().resolveObserverMethods(new Temperature(0d));
            assertEquals(1, temperatureObservers.size());
            ObserverMethod<? super Temperature> temperatureObserver = temperatureObservers.iterator().next();
            assertEquals(AirConditioner.class, temperatureObserver.getBeanClass());
            assertEquals(Temperature.class, temperatureObserver.getObservedType());

            Method method = AirConditioner.class.getMethod("temperatureChanged", Temperature.class);
            assertNotNull(method);
            assertEquals(1, method.getParameterTypes().length);
            assertEquals(Temperature.class, method.getParameterTypes()[0]);
            assertEquals(Observes.class, method.getParameterAnnotations()[0][0].annotationType());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodWithoutBindingTypesObservesEventsWithoutBindingTypes() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(2, syringe.getBeanManager().resolveObserverMethods(new SimpleEventType()).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodMayHaveMultipleBindingTypes() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(2, syringe.getBeanManager().resolveObserverMethods(new MultiBindingEvent(),
                    new RoleBinding("Admin"), new TameAnnotationLiteral()).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodRegistration() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(2, syringe.getBeanManager().resolveObserverMethods(new SimpleEventType()).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanManagerResolveObserversSignature() throws Exception {
        Syringe syringe = newSyringe();
        try {
            assertNotNull(syringe.getBeanManager().getClass().getDeclaredMethod(
                    BEAN_MANAGER_RESOLVE_OBSERVERS_METHOD_NAME, Object.class, Annotation[].class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanManagerResolveObserversWithIllegalQualifier() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class, () ->
                    syringe.getBeanManager().resolveObserverMethods(new SimpleEventType(), OverrideLiteral.INSTANCE));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodAutomaticallyRegistered() {
        Syringe syringe = newSyringe();
        try {
            assertFalse(syringe.getBeanManager().resolveObserverMethods(new String(), new Secret.Literal()).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodNotAutomaticallyRegisteredForDisabledBeans() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super Ghost>> ghostObservers = syringe.getBeanManager().resolveObserverMethods(new Ghost());
            assertEquals(0, ghostObservers.size());

            Set<ObserverMethod<? super String>> stringObservers =
                    syringe.getBeanManager().resolveObserverMethods(new String(), new Secret.Literal());
            assertEquals(1, stringObservers.size());
            for (ObserverMethod<? super String> observer : stringObservers) {
                observer.notify("fail if disabled observer invoked");
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSyncObserver() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super DiskSpaceEvent>> diskSpaceObservers =
                    syringe.getBeanManager().resolveObserverMethods(new DiskSpaceEvent());
            assertTrue(diskSpaceObservers.stream().allMatch(method -> !method.isAsync()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAsyncObserver() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super User>> userObservers = syringe.getBeanManager().resolveObserverMethods(new User());
            assertEquals(1, userObservers.size());
            assertTrue(userObservers.iterator().next().isAsync());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                AirConditioner.class,
                BatteryEvent.class,
                BullTerrier.class,
                Cloud.class,
                DisabledObserver.class,
                DiskSpaceEvent.class,
                Ghost.class,
                MultiBindingEvent.class,
                NotEnabled.class,
                Pomeranian.class,
                PriviledgedObserver.class,
                Role.class,
                RoleBinding.class,
                Secret.class,
                SimpleEventType.class,
                SystemMonitor.class,
                Tame.class,
                TameAnnotationLiteral.class,
                Temperature.class,
                Thermostat.class,
                User.class,
                OverrideLiteral.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @Dependent
    static class AirConditioner {
        private Temperature target;
        private boolean on = false;

        public void setTargetTemperature(Temperature target) {
            this.target = target;
        }

        public void temperatureChanged(@Observes Temperature temperature) {
            if (on && temperature.getDegrees() <= target.getDegrees()) {
                on = false;
            } else if (!on && temperature.getDegrees() > target.getDegrees()) {
                on = true;
            }
        }

        public boolean isOn() {
            return on;
        }
    }

    static class BatteryEvent {
    }

    @Dependent
    static class BullTerrier {
        private static boolean multiBindingEventObserved = false;
        private static boolean singleBindingEventObserved = false;

        public void observesMultiBindingEvent(@Observes @Role("Admin") @Tame MultiBindingEvent someEvent) {
            multiBindingEventObserved = true;
        }

        public void observesSingleBindingEvent(@Observes @Tame MultiBindingEvent someEvent) {
            singleBindingEventObserved = true;
        }

        public static boolean isMultiBindingEventObserved() {
            return multiBindingEventObserved;
        }

        public static boolean isSingleBindingEventObserved() {
            return singleBindingEventObserved;
        }

        public static void reset() {
            multiBindingEventObserved = false;
            singleBindingEventObserved = false;
        }
    }

    @Dependent
    static class Cloud {
        public void allocateNewDisk(@Observes DiskSpaceEvent event) {
        }
    }

    @NotEnabled
    @Dependent
    static class DisabledObserver {
        public void observeSecret(@Observes @Secret String secretString) {
            if ("fail if disabled observer invoked".equals(secretString)) {
                throw new AssertionError("This observer should not be invoked since it resides on a bean with a policy that is not enabled.");
            }
        }

        public void observeGhost(@Observes Ghost ghost) {
            throw new AssertionError("This observer should not be invoked since it resides on a bean with a policy that is not enabled.");
        }
    }

    static class DiskSpaceEvent {
    }

    static class Ghost {
    }

    static class MultiBindingEvent {
    }

    @Stereotype
    @Alternative
    @Target({TYPE, METHOD})
    @Retention(RUNTIME)
    public @interface NotEnabled {
    }

    @Named("Teddy")
    @Dependent
    static class Pomeranian {
        static Thread notificationThread;

        public void observeSimpleEvent(@Observes SimpleEventType someEvent) {
            notificationThread = Thread.currentThread();
        }

        public void observerTameSimpleEvent(@Observes @Tame SimpleEventType someEvent) {
        }

        public static void staticallyObserveEvent(@Observes SimpleEventType someEvent) {
        }
    }

    @Dependent
    static class PriviledgedObserver {
        public void observeSecret(@Observes @Secret String secretString) {
        }
    }

    @Target({FIELD, PARAMETER, METHOD, TYPE})
    @Retention(RUNTIME)
    @Qualifier
    public @interface Role {
        String value();
    }

    static class RoleBinding extends AnnotationLiteral<Role> implements Role {
        private static final long serialVersionUID = 1L;
        private final String value;

        RoleBinding(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Secret {
        class Literal extends AnnotationLiteral<Secret> implements Secret {
            private static final long serialVersionUID = 1L;
        }
    }

    static class SimpleEventType {
    }

    @Dependent
    static class SystemMonitor {
        public void lowBattery(@Observes BatteryEvent event) {
        }

        public void lowDiskSpace(@Observes DiskSpaceEvent event) {
        }

        public void newUserAdded(@ObservesAsync User user) {
        }
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface Tame {
    }

    static class TameAnnotationLiteral extends AnnotationLiteral<Tame> implements Tame {
        private static final long serialVersionUID = 1L;
    }

    static class Temperature {
        private final double degrees;

        Temperature(double degrees) {
            this.degrees = degrees;
        }

        public double getDegrees() {
            return degrees;
        }
    }

    static class Thermostat {
        public void notifyTemperatureChanged(Temperature temperature) {
        }
    }

    static class User {
    }

    static class OverrideLiteral extends AnnotationLiteral<Override> implements Override {
        private static final long serialVersionUID = 1L;
        static final OverrideLiteral INSTANCE = new OverrideLiteral();
    }
}
