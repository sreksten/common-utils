package com.threeamigos.common.util.implementations.injection.cditcktests.event.resolve.typeWithParameters;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class CheckTypeParametersWhenResolvingObserversTest {

    @Test
    void testResolvingChecksTypeParameters() {
        Syringe syringe = newSyringe();
        try {
            verifyObserver(syringe, new StringList(), 1, StringListObserver.class);
            verifyObserver(syringe, new IntegerList(), 1, IntegerListObserver.class);
            verifyObserver(syringe, new CharacterList(), 0);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testParameterizedEventTypeAssignableToRawType() {
        Syringe syringe = newSyringe();
        try {
            Event<Box<Integer, String, Random>> event = syringe.getBeanManager().getEvent()
                    .select(new TypeLiteral<Box<Integer, String, Random>>() {
                    });
            Box<Integer, String, Random> box = new Box<Integer, String, Random>();
            RawTypeObserver.OBSERVED = false;
            event.fire(box);
            assertTrue(RawTypeObserver.OBSERVED);
            verifyObserver(syringe, new RawTypeObserver.BoxWithDifferentTypeParameters(), 1, RawTypeObserver.class);
            verifyObserver(syringe, new RawTypeObserver.BoxWithObjectTypeParameters(), 1, RawTypeObserver.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObservedEventTypeParameterIsActualType() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            Foo<String> fooString = new Foo.FooString();
            verifyObserver(syringe, fooString, 1, FooObserver.class);
            syringe.getBeanManager().getEvent().select(new TypeLiteral<Foo<String>>() {
            }).fire(fooString);
            verifyEvent(FooObserver.SEQUENCE, 1, AbstractObserver.buildActionId(FooObserver.class, fooString));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObservedEventTypeParameterIsActualTypeNested() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            Foo<List<String>> fooStringList = new Foo.FooStringList();
            verifyObserver(syringe, fooStringList, 1, FooObserver.class);
            syringe.getBeanManager().getEvent().select(new TypeLiteral<Foo<List<String>>>() {
            }).fire(fooStringList);
            verifyEvent(FooObserver.SEQUENCE_NESTED, 1, AbstractObserver.buildActionId(FooObserver.class, fooStringList));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObservedEventTypeParameterIsWildcard() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();

            Qux<String> quxString = new Qux.QuxString();
            Qux<List<String>> quxStringList = new Qux.QuxStringList();
            Qux<Number> quxNumber = new Qux.QuxNumber();

            verifyObserver(syringe, quxString, 1, WildcardObserver.class);
            verifyObserver(syringe, quxNumber, 2, WildcardObserver.class);
            verifyObserver(syringe, quxStringList, 2, WildcardObserver.class);

            syringe.getBeanManager().getEvent().select(new TypeLiteral<Qux<String>>() {
            }).fire(quxString);
            syringe.getBeanManager().getEvent().select(new TypeLiteral<Qux<List<String>>>() {
            }).fire(quxStringList);
            syringe.getBeanManager().getEvent().select(new TypeLiteral<Qux<Number>>() {
            }).fire(quxNumber);

            verifyEvent(WildcardObserver.SEQUENCE, 3,
                    AbstractObserver.buildActionId(WildcardObserver.class, quxString),
                    AbstractObserver.buildActionId(WildcardObserver.class, quxStringList),
                    AbstractObserver.buildActionId(WildcardObserver.class, quxNumber));
            verifyEvent(WildcardObserver.SEQUENCE_LOWER, 1,
                    AbstractObserver.buildActionId(WildcardObserver.class, quxNumber));
            verifyEvent(WildcardObserver.SEQUENCE_UPPER, 1,
                    AbstractObserver.buildActionId(WildcardObserver.class, quxStringList));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObservedEventTypeParameterIsTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();

            Duck<String> duckString = new Duck.DuckString();
            Duck<Integer> duckInteger = new Duck.DuckInteger();

            verifyObserver(syringe, duckString, 1, TypeVariableObserver.class);
            verifyObserver(syringe, duckInteger, 2, TypeVariableObserver.class);

            syringe.getBeanManager().getEvent().select(new TypeLiteral<Duck<String>>() {
            }).fire(duckString);
            syringe.getBeanManager().getEvent().select(new TypeLiteral<Duck<Integer>>() {
            }).fire(duckInteger);

            verifyEvent(TypeVariableObserver.SEQUENCE_TYPE_VAR, 2,
                    AbstractObserver.buildActionId(TypeVariableObserver.class, duckString),
                    AbstractObserver.buildActionId(TypeVariableObserver.class, duckInteger));
            verifyEvent(TypeVariableObserver.SEQUENCE_TYPE_VAR_UPPER, 1,
                    AbstractObserver.buildActionId(TypeVariableObserver.class, duckInteger));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventTypeAssignableToATypeVariable() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();

            Bar bar = new Bar();
            Baz baz = new Baz();

            verifyObserver(syringe, bar, 1, TypeVariableObserver.class);
            verifyObserver(syringe, baz, 1, TypeVariableObserver.class);

            syringe.getBeanManager().getEvent().select(Bar.class).fire(new Bar());
            syringe.getBeanManager().getEvent().select(Baz.class).fire(new Baz());
            syringe.getBeanManager().getEvent().select(StringList.class).fire(new StringList());

            verifyEvent(TypeVariableObserver.SEQUENCE_UPPER, 2,
                    AbstractObserver.buildActionId(TypeVariableObserver.class, bar),
                    AbstractObserver.buildActionId(TypeVariableObserver.class, baz));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventTypeResolution() {
        Syringe syringe = newSyringe();
        try {
            int expectedMatches = 5;
            Dog<?, ?> dogStringNumber = new Dog.DogStringNumber();
            verifyObserver(syringe, dogStringNumber, expectedMatches, DogObserver.class);
            syringe.getBeanManager().getEvent().select(new TypeLiteral<Dog<?, ?>>() {
            }).fire(dogStringNumber);
            verifyEvent(DogObserver.SEQUENCE, expectedMatches);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Box.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Baz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Qux.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Duck.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Dog.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StringListObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntegerListObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(RawTypeObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FooObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WildcardObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TypeVariableObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DogObserver.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    private void verifyObserver(Syringe syringe,
                                Object event,
                                int expectedNumberOfObservers,
                                Class<?>... expectedObserverTypes) {
        if (expectedNumberOfObservers < expectedObserverTypes.length) {
            throw new IllegalArgumentException("Invalid expected arguments");
        }

        Set<ObserverMethod<? super Object>> observers = syringe.getBeanManager().resolveObserverMethods(event);
        assertEquals(expectedNumberOfObservers, observers.size());

        List<Class<?>> observerTypes = Arrays.asList(expectedObserverTypes);
        for (ObserverMethod<? super Object> observer : observers) {
            assertTrue(observerTypes.contains(observer.getBeanClass()));
        }
    }

    private void verifyEvent(String sequenceName, int expectedSequenceSize, String... actions) {
        List<String> sequenceData = ActionSequence.getSequenceData(sequenceName);
        assertNotNull(sequenceData);
        assertEquals(expectedSequenceSize, sequenceData.size());

        for (String action : actions) {
            assertTrue(sequenceData.contains(action));
        }
    }

    static class CharacterList extends ArrayList<Character> {
        private static final long serialVersionUID = 1L;
    }

    static class StringList extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
    }

    static class IntegerList extends ArrayList<Integer> {
        private static final long serialVersionUID = 1L;
    }

    @Dependent
    static class StringListObserver {
        boolean wasNotified = false;

        void observer(@Observes ArrayList<String> event) {
            wasNotified = true;
        }
    }

    @Dependent
    static class IntegerListObserver {
        boolean wasNotified = false;

        void observer(@Observes ArrayList<Integer> event) {
            wasNotified = true;
        }
    }

    static class Box<A, B, C> {
    }

    static class Bar {
    }

    static class Baz extends Bar {
    }

    static class UnusedEventType {
        UnusedEventType(String name) {
        }
    }

    static class Foo<T> {
        protected static class FooString extends Foo<String> {
        }

        protected static class FooInteger extends Foo<Integer> {
        }

        protected static class FooStringList extends Foo<List<String>> {
        }

        protected static class FooObject extends Foo<Object> {
        }
    }

    static class Qux<T> {
        protected static class QuxString extends Qux<String> {
        }

        protected static class QuxNumber extends Qux<Number> {
        }

        protected static class QuxStringList extends Qux<List<String>> {
        }
    }

    static class Duck<T> {
        protected static class DuckString extends Duck<String> {
        }

        protected static class DuckInteger extends Duck<Integer> {
        }
    }

    static class Dog<T, K> {
        static class DogStringNumber extends Dog<String, Number> {
        }
    }

    abstract static class AbstractObserver {
        protected void addAction(String sequenceName, Object event) {
            ActionSequence.addAction(sequenceName, buildActionId(getClass(), event));
        }

        @SuppressWarnings("rawtypes")
        static String buildActionId(Class observerClazz, Object eventObject) {
            return observerClazz.getName() + eventObject.getClass().getName();
        }
    }

    @Dependent
    static class RawTypeObserver {
        static boolean OBSERVED = false;

        @SuppressWarnings("rawtypes")
        void observe(@Observes Box box) {
            OBSERVED = true;
        }

        static class BoxWithObjectTypeParameters extends Box<Object, Object, Object> {
        }

        static class BoxWithDifferentTypeParameters extends Box<Number, String, Random> {
        }
    }

    @Dependent
    static class FooObserver extends AbstractObserver {
        static final String SEQUENCE = "fooString";
        static final String SEQUENCE_NESTED = "fooStringList";

        void observeTypeParameterActualType(@Observes Foo<String> foo) {
            addAction(SEQUENCE, foo);
        }

        void observeTypeParameterActualTypeNested(@Observes Foo<List<String>> foo) {
            addAction(SEQUENCE_NESTED, foo);
        }
    }

    @Dependent
    static class WildcardObserver extends AbstractObserver {
        static final String SEQUENCE = "quxWildcard";
        static final String SEQUENCE_UPPER = "quxWildcardUpper";
        static final String SEQUENCE_LOWER = "quxWildcardLower";

        void observeTypeParameterWildcard(@Observes Qux<?> qux) {
            addAction(SEQUENCE, qux);
        }

        @SuppressWarnings("rawtypes")
        void observeTypeParameterWildcardUpper(@Observes Qux<? extends List> qux) {
            addAction(SEQUENCE_UPPER, qux);
        }

        void observeTypeParameterWildcardLower(@Observes Qux<? super Integer> qux) {
            addAction(SEQUENCE_LOWER, qux);
        }
    }

    @Dependent
    static class TypeVariableObserver extends AbstractObserver {
        static final String SEQUENCE_UPPER = "barTypeVariable";
        static final String SEQUENCE_TYPE_VAR = "duckTypeVariable";
        static final String SEQUENCE_TYPE_VAR_UPPER = "duckWildcardUpper";

        <T extends Bar> void observeAnyBar(@Observes T anyBar) {
            addAction(SEQUENCE_UPPER, anyBar);
        }

        <T> void observeTypeParameterTypeVariable(@Observes Duck<T> duck) {
            addAction(SEQUENCE_TYPE_VAR, duck);
        }

        <T extends Number> void observeTypeParameterTypeVariableUpper(@Observes Duck<T> duck) {
            addAction(SEQUENCE_TYPE_VAR_UPPER, duck);
        }
    }

    @Dependent
    static class DogObserver extends AbstractObserver {
        static final String SEQUENCE = "dog";

        @SuppressWarnings("rawtypes")
        void observeDogRaw(@Observes Dog dog) {
            addAction(SEQUENCE, dog);
        }

        void observeDogWildcard(@Observes Dog<?, ?> dog) {
            addAction(SEQUENCE, dog);
        }

        void observeDogWildcardLower(@Observes Dog<String, ? super Integer> dog) {
            addAction(SEQUENCE, dog);
        }

        void observeDogWildcardUpperLower(@Observes Dog<? extends Object, ? super Integer> dog) {
            addAction(SEQUENCE, dog);
        }

        <T extends Number> void observeDogTypeVariableUpper(@Observes Dog<String, T> dog) {
            addAction(SEQUENCE, dog);
        }
    }

    static class ActionSequence {
        private static final Map<String, List<String>> SEQUENCES = new ConcurrentHashMap<String, List<String>>();

        static void reset() {
            SEQUENCES.clear();
        }

        static void addAction(String sequenceName, String action) {
            List<String> sequence = SEQUENCES.get(sequenceName);
            if (sequence == null) {
                sequence = new CopyOnWriteArrayList<String>();
                SEQUENCES.put(sequenceName, sequence);
            }
            sequence.add(action);
        }

        static List<String> getSequenceData(String sequenceName) {
            List<String> sequence = SEQUENCES.get(sequenceName);
            return sequence == null ? new ArrayList<String>() : new ArrayList<String>(sequence);
        }
    }
}
