package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.priority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.interceptor.Interceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class EventObserverOrderingTest {

    @Test
    void testFireEventLowerPriorityBeforeDefaultPriority() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            syringe.getBeanManager().getEvent().select(Sunrise.class).fire(new Sunrise());

            assertEquals(3, ActionSequence.getSequenceSize());
            assertTrue(ActionSequence.beginsWith(SunriseObservers.AsianObserver.class.getName()));
            ActionSequence.assertSequenceDataContainsAll(SunriseObservers.GermanObserver.class.getName(),
                    SunriseObservers.ItalianObserver.class.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveObserversLowerPriorityBeforeDefaultPriority() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super Sunrise>> observerMethods = syringe.getBeanManager().resolveObserverMethods(new Sunrise());

            assertEquals(3, observerMethods.size());
            assertEquals(SunriseObservers.AsianObserver.class, observerMethods.iterator().next().getBeanClass());

            List<Class<?>> classes = observerMethods.stream().map(ObserverMethod::getBeanClass).collect(Collectors.toList());
            assertTrue(classes.contains(SunriseObservers.GermanObserver.class));
            assertTrue(classes.contains(SunriseObservers.ItalianObserver.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testFireEventToMultipleObserversWithPriority() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            syringe.getBeanManager().getEvent().select(Sunset.class).fire(new Sunset());

            assertEquals(3, ActionSequence.getSequenceSize());
            assertTrue(ActionSequence.beginsWith(SunsetObservers.AsianObserver.class.getName(),
                    SunsetObservers.EuropeanObserver.class.getName(), SunsetObservers.AmericanObserver.class.getName()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPrioritizedEventSubclass() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            syringe.getBeanManager().getEvent().select(Moonrise.class).fire(new Moonrise());

            assertEquals(4, ActionSequence.getSequenceSize());
            ActionSequence.assertSequenceDataEquals(MoonObservers.Observer1.class.getName(),
                    MoonObservers.Observer2.class.getName(), MoonObservers.Observer3.class.getName(),
                    MoonObservers.Observer4.class.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPrioritizedEventBaseclass() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            syringe.getBeanManager().getEvent().select(MoonActivity.class).fire(new MoonActivity());

            assertEquals(2, ActionSequence.getSequenceSize());
            ActionSequence.assertSequenceDataEquals(MoonObservers.Observer1.class.getName(),
                    MoonObservers.Observer3.class.getName());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Sunrise.class,
                Sunset.class,
                MoonActivity.class,
                Moonrise.class,
                SunriseObservers.AsianObserver.class,
                SunriseObservers.GermanObserver.class,
                SunriseObservers.ItalianObserver.class,
                SunsetObservers.AsianObserver.class,
                SunsetObservers.EuropeanObserver.class,
                SunsetObservers.AmericanObserver.class,
                MoonObservers.Observer1.class,
                MoonObservers.Observer2.class,
                MoonObservers.Observer3.class,
                MoonObservers.Observer4.class,
                ActionSequence.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    static class Sunrise {
    }

    static class Sunset {
    }

    static class MoonActivity {
    }

    static class Moonrise extends MoonActivity {
    }

    static class SunriseObservers {
        @Dependent
        static class AsianObserver {
            public void observeSunrise(@Observes @Priority(Interceptor.Priority.APPLICATION + 499) Sunrise sunrise) {
                ActionSequence.addAction(getClass().getName());
            }
        }

        @Dependent
        static class GermanObserver {
            public void observeSunrise(@Observes Sunrise sunrise) {
                ActionSequence.addAction(getClass().getName());
            }
        }

        @Dependent
        static class ItalianObserver {
            public void observeSunrise(@Observes Sunrise sunrise) {
                ActionSequence.addAction(getClass().getName());
            }
        }
    }

    static class SunsetObservers {
        @Dependent
        static class AsianObserver {
            public void observeSunset(@Observes @Priority(2599) Sunset sunset) {
                ActionSequence.addAction(getClass().getName());
            }
        }

        @Dependent
        static class EuropeanObserver {
            public void observeSunset(@Observes @Priority(2600) Sunset sunset) {
                ActionSequence.addAction(getClass().getName());
            }
        }

        @Dependent
        static class AmericanObserver {
            public void observeSunset(@Observes @Priority(2700) Sunset sunset) {
                ActionSequence.addAction(getClass().getName());
            }
        }
    }

    static class MoonObservers {
        @Dependent
        static class Observer1 {
            public void observeMoon(@Observes @Priority(Interceptor.Priority.APPLICATION) MoonActivity moonActivity) {
                ActionSequence.addAction(getClass().getName());
            }
        }

        @Dependent
        static class Observer2 {
            public void observeMoon(@Observes Moonrise moonrise) {
                ActionSequence.addAction(getClass().getName());
            }
        }

        @Dependent
        static class Observer3 {
            public void observeMoon(@Observes @Priority(Interceptor.Priority.APPLICATION + 900) MoonActivity moonActivity) {
                ActionSequence.addAction(getClass().getName());
            }
        }

        @Dependent
        static class Observer4 {
            public void observeMoon(@Observes @Priority(Interceptor.Priority.APPLICATION + 950) Moonrise moonrise) {
                ActionSequence.addAction(getClass().getName());
            }
        }
    }

    static class ActionSequence {
        private static final List<String> ACTIONS = new CopyOnWriteArrayList<String>();

        static void reset() {
            ACTIONS.clear();
        }

        static void addAction(String action) {
            ACTIONS.add(action);
        }

        static int getSequenceSize() {
            return ACTIONS.size();
        }

        static boolean beginsWith(String... expectedPrefix) {
            if (ACTIONS.size() < expectedPrefix.length) {
                return false;
            }
            for (int i = 0; i < expectedPrefix.length; i++) {
                if (!expectedPrefix[i].equals(ACTIONS.get(i))) {
                    return false;
                }
            }
            return true;
        }

        static void assertSequenceDataContainsAll(String... expected) {
            assertTrue(ACTIONS.containsAll(Arrays.asList(expected)));
        }

        static void assertSequenceDataEquals(String... expected) {
            List<String> expectedList = new ArrayList<String>(Arrays.asList(expected));
            assertEquals(expectedList, new ArrayList<String>(ACTIONS));
        }
    }
}
