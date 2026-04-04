package com.threeamigos.common.util.implementations.injection.cditcktests.event.parameterized;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class ParameterizedEventTest {

    @Test
    void testSelectedEventTypeUsedForResolvingEventTypeArguments() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            reset(ctx);

            ctx.integerListBarEvent.fire(new Bar<List<Integer>>());

            assertTrue(ctx.observer.isIntegerListFooableObserved());
            assertTrue(ctx.observer.isIntegerListFooObserved());
            assertTrue(ctx.observer.isIntegerListBarObserved());
            assertFalse(ctx.observer.isBazObserved());
            assertFalse(ctx.observer.isStringListFooableObserved());

            assertTrue(ctx.integerObserver.isFooableObserved());
            assertTrue(ctx.integerObserver.isFooObserved());
            assertTrue(ctx.integerObserver.isBarObserved());

            assertFalse(ctx.stringObserver.isFooableObserved());
            assertFalse(ctx.stringObserver.isFooObserved());
            assertFalse(ctx.stringObserver.isBarObserved());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSelectedEventTypeUsedForResolvingEventTypeArguments2() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            reset(ctx);

            Event<Foo<List<Integer>>> selectedEvent = ctx.event.select(new TypeLiteral<Foo<List<Integer>>>() {
            });
            selectedEvent.fire(new Foo<List<Integer>>());

            assertTrue(ctx.observer.isIntegerListFooableObserved());
            assertTrue(ctx.observer.isIntegerListFooObserved());
            assertFalse(ctx.observer.isIntegerListBarObserved());
            assertFalse(ctx.observer.isBazObserved());
            assertFalse(ctx.observer.isStringListFooableObserved());

            assertTrue(ctx.integerObserver.isFooableObserved());
            assertTrue(ctx.integerObserver.isFooObserved());
            assertFalse(ctx.integerObserver.isBarObserved());

            assertFalse(ctx.stringObserver.isFooableObserved());
            assertFalse(ctx.stringObserver.isFooObserved());
            assertFalse(ctx.stringObserver.isBarObserved());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSelectedEventTypeCombinedWithEventObjectRuntimeTypeForResolvingEventTypeArguments() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            reset(ctx);

            Event<Foo<List<Integer>>> selectedEvent = ctx.event.select(new TypeLiteral<Foo<List<Integer>>>() {
            });
            selectedEvent.fire(new Bar<List<Integer>>());

            assertTrue(ctx.observer.isIntegerListFooableObserved());
            assertTrue(ctx.observer.isIntegerListFooObserved());
            assertTrue(ctx.observer.isIntegerListBarObserved());
            assertFalse(ctx.observer.isBazObserved());
            assertFalse(ctx.observer.isStringListFooableObserved());

            assertTrue(ctx.integerObserver.isFooableObserved());
            assertTrue(ctx.integerObserver.isFooObserved());
            assertTrue(ctx.integerObserver.isBarObserved());

            assertFalse(ctx.stringObserver.isFooableObserved());
            assertFalse(ctx.stringObserver.isFooObserved());
            assertFalse(ctx.stringObserver.isBarObserved());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSelectedEventTypeCombinedWithEventObjectRuntimeTypeForResolvingEventTypeArguments2() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            reset(ctx);

            Event<List<Character>> selectedEvent = ctx.event.select(new TypeLiteral<List<Character>>() {
            });
            selectedEvent.fire(new ArrayList<Character>());

            assertTrue(ctx.observer.isCharacterListObserved());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventObjectTypeUsed() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            reset(ctx);

            ctx.integerListBarEvent.fire(new Baz());

            assertTrue(ctx.observer.isIntegerListFooableObserved());
            assertTrue(ctx.observer.isIntegerListFooObserved());
            assertTrue(ctx.observer.isIntegerListBarObserved());
            assertTrue(ctx.observer.isBazObserved());
            assertFalse(ctx.observer.isStringListFooableObserved());

            assertTrue(ctx.integerObserver.isFooableObserved());
            assertTrue(ctx.integerObserver.isFooObserved());
            assertTrue(ctx.integerObserver.isBarObserved());

            assertFalse(ctx.stringObserver.isFooableObserved());
            assertFalse(ctx.stringObserver.isFooObserved());
            assertFalse(ctx.stringObserver.isBarObserved());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testUnresolvedTypeVariableDetected1() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            assertThrows(IllegalArgumentException.class,
                    () -> ctx.integerListFooEvent.fire(new Blah<List<Integer>, Integer>()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testUnresolvedTypeVariableDetected2() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            assertThrows(IllegalArgumentException.class, () -> fireMapWithTypeVariable(ctx.event));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testUnresolvedTypeVariableDetected3() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            assertThrows(IllegalArgumentException.class, () -> fireDeepListWithTypeVariable(ctx.event));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testWildcardIsResolvable() {
        Syringe syringe = newSyringe();
        try {
            EventContext ctx = getEventContext(syringe);
            reset(ctx);
            ctx.fooEvent.fire(new Bar<Integer>());
            assertTrue(ctx.observer.isIntegerFooObserved());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(EventObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerListObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StringListObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    private EventContext getEventContext(Syringe syringe) {
        jakarta.enterprise.inject.spi.BeanManager beanManager = syringe.getBeanManager();
        EventContext ctx = new EventContext();
        ctx.event = beanManager.getEvent();
        ctx.integerListFooEvent = beanManager.getEvent().select(new TypeLiteral<Foo<List<Integer>>>() {
        });
        ctx.integerListBarEvent = beanManager.getEvent().select(new TypeLiteral<Bar<List<Integer>>>() {
        });
        ctx.fooEvent = beanManager.getEvent().select(new TypeLiteral<Foo<? extends Number>>() {
        });
        ctx.observer = getContextualReference(beanManager, EventObserver.class);
        ctx.integerObserver = getContextualReference(beanManager, IntegerListObserver.class);
        ctx.stringObserver = getContextualReference(beanManager, StringListObserver.class);
        return ctx;
    }

    private void reset(EventContext ctx) {
        ctx.observer.reset();
        ctx.integerObserver.reset();
        ctx.stringObserver.reset();
    }

    private <T> void fireMapWithTypeVariable(Event<Object> event) {
        event.select(new TypeLiteral<Map<Exception, T>>() {
        }).fire(new HashMap<Exception, T>());
    }

    private <T> void fireDeepListWithTypeVariable(Event<Object> event) {
        event.select(new TypeLiteral<ArrayList<List<List<List<T>>>>>() {
        }).fire(new ArrayList<List<List<List<T>>>>());
    }

    private static <T> T getContextualReference(jakarta.enterprise.inject.spi.BeanManager beanManager, Class<T> type) {
        jakarta.enterprise.inject.spi.Bean<?> bean = beanManager.resolve(beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    private static class EventContext {
        private Event<Object> event;
        private Event<Foo<List<Integer>>> integerListFooEvent;
        private Event<Bar<List<Integer>>> integerListBarEvent;
        private Event<Foo<? extends Number>> fooEvent;
        private EventObserver observer;
        private IntegerListObserver integerObserver;
        private StringListObserver stringObserver;
    }

    interface Fooable<F> {
    }

    static class Foo<F> implements Fooable<F> {
    }

    static class Bar<B> extends Foo<B> {
    }

    static class Baz extends Bar<List<Integer>> {
    }

    static class Blah<B1, B2> extends Bar<B1> {
    }

    abstract static class AbstractParameterizedObserver<T> {
        private boolean fooableObserved;
        private boolean fooObserved;
        private boolean barObserved;

        protected void observeFooable(@Observes Fooable<T> event) {
            fooableObserved = true;
        }

        protected void observeFoo(@Observes Foo<T> event) {
            fooObserved = true;
        }

        protected void observeBar(@Observes Bar<T> event) {
            barObserved = true;
        }

        boolean isFooableObserved() {
            return fooableObserved;
        }

        boolean isFooObserved() {
            return fooObserved;
        }

        boolean isBarObserved() {
            return barObserved;
        }

        void reset() {
            fooableObserved = false;
            fooObserved = false;
            barObserved = false;
        }
    }

    @ApplicationScoped
    static class IntegerListObserver extends AbstractParameterizedObserver<List<Integer>> {
    }

    @ApplicationScoped
    static class StringListObserver extends AbstractParameterizedObserver<List<String>> {
    }

    @ApplicationScoped
    static class EventObserver {
        private boolean integerListFooableObserved;
        private boolean stringListFooableObserved;
        private boolean integerListFooObserved;
        private boolean integerListBarObserved;
        private boolean bazObserved;
        private boolean characterListObserved;
        private boolean integerFooObserved;

        void observeIntegerFooable(@Observes Fooable<List<Integer>> event) {
            integerListFooableObserved = true;
        }

        void observeStringFooable(@Observes Fooable<List<String>> event) {
            stringListFooableObserved = true;
        }

        void observeListIntegerFoo(@Observes Foo<List<Integer>> event) {
            integerListFooObserved = true;
        }

        void observeIntegerBar(@Observes Bar<List<Integer>> event) {
            integerListBarObserved = true;
        }

        void observeBaz(@Observes Baz baz) {
            bazObserved = true;
        }

        void observeCharacterList(@Observes List<Character> event) {
            characterListObserved = true;
        }

        void observeIntegerFoo(@Observes Foo<? extends Number> event) {
            integerFooObserved = true;
        }

        boolean isStringListFooableObserved() {
            return stringListFooableObserved;
        }

        boolean isIntegerListFooObserved() {
            return integerListFooObserved;
        }

        boolean isIntegerListBarObserved() {
            return integerListBarObserved;
        }

        boolean isIntegerListFooableObserved() {
            return integerListFooableObserved;
        }

        boolean isBazObserved() {
            return bazObserved;
        }

        boolean isCharacterListObserved() {
            return characterListObserved;
        }

        boolean isIntegerFooObserved() {
            return integerFooObserved;
        }

        void reset() {
            integerListFooableObserved = false;
            stringListFooableObserved = false;
            integerListBarObserved = false;
            integerListFooObserved = false;
            bazObserved = false;
            characterListObserved = false;
            integerFooObserved = false;
        }
    }
}
