package com.threeamigos.common.util.implementations.injection.cditcktests.event.eventTypes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTypesTest {

    private final AnnotationLiteral<Extra> extraLiteral = new Extra.Literal();

    @Test
    void testEventTypeIsConcreteTypeWithNoTypeVariables() {
        Syringe syringe = newSyringe();
        try {
            Listener listener = getContextualReference(syringe, Listener.class);
            listener.reset();

            Song song = new Song();
            getContextualReference(syringe, TuneSelect.class).songPlaying(song);
            assertEquals(1, listener.getObjectsFired().size());
            assertTrue(listener.getObjectsFired().get(0) == song);

            getContextualReference(syringe, EventTypesProbe.class).songEvent.fire(song);
            assertEquals(2, listener.getObjectsFired().size());
            assertTrue(listener.getObjectsFired().get(1) == song);

            Broadcast broadcast = new Broadcast() {
            };
            getContextualReference(syringe, TuneSelect.class).broadcastPlaying(broadcast);
            assertEquals(3, listener.getObjectsFired().size());
            assertTrue(listener.getObjectsFired().get(2) == broadcast);

            syringe.getBeanManager().getEvent().select(extraLiteral).fire(Integer.valueOf(1));
            assertEquals(4, listener.getObjectsFired().size());
            assertEquals(Integer.valueOf(1), listener.getObjectsFired().get(3));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventTypeIsArray() {
        Syringe syringe = newSyringe();
        try {
            Listener listener = getContextualReference(syringe, Listener.class);
            listener.reset();

            Song[] songArray = new Song[]{new Song()};
            getContextualReference(syringe, EventTypesProbe.class).songArrayEvent.fire(songArray);
            assertEquals(1, listener.getObjectsFired().size());
            assertTrue(listener.getObjectsFired().get(0) instanceof Song[]);
            assertEquals(songArray, listener.getObjectsFired().get(0));

            Integer[] integerArray = new Integer[]{Integer.valueOf(0), Integer.valueOf(1)};
            syringe.getBeanManager().getEvent().select(Integer[].class).fire(integerArray);
            assertEquals(2, listener.getObjectsFired().size());
            assertTrue(listener.getObjectsFired().get(1) instanceof Integer[]);
            assertEquals(integerArray, listener.getObjectsFired().get(1));

            int[] intArray = new int[]{1, 2};
            getContextualReference(syringe, EventTypesProbe.class).intArrayEvent.fire(intArray);
            assertEquals(3, listener.getObjectsFired().size());
            assertTrue(listener.getObjectsFired().get(2) instanceof int[]);
            assertEquals(intArray, listener.getObjectsFired().get(2));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testEventTypeIncludesAllSuperclassesAndInterfacesOfEventObject() {
        Syringe syringe = newSyringe();
        try {
            EventTypeFamilyObserver observer = getContextualReference(syringe, EventTypeFamilyObserver.class);
            observer.reset();

            syringe.getBeanManager().getEvent().select(ComplexEvent.class).fire(new ComplexEvent());
            assertEquals(1, observer.getGeneralEventQuantity());
            assertEquals(1, observer.getAbstractEventQuantity());
            assertEquals(1, observer.getComplexEventQuantity());
            assertEquals(1, observer.getObjectEventQuantity());
            assertEquals(4, observer.getTotalEventsObserved());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                EventTypesProbe.class,
                EventTypeFamilyObserver.class,
                Listener.class,
                TuneSelect.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        ((BeanManagerImpl) syringe.getBeanManager()).getContextManager().activateRequest();
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanType) {
        return syringe.getBeanManager().createInstance().select(beanType).get();
    }

    @Dependent
    static class EventTypesProbe {
        @Inject
        Event<Song> songEvent;

        @Inject
        Event<Song[]> songArrayEvent;

        @Inject
        Event<int[]> intArrayEvent;
    }

    @RequestScoped
    static class Listener {
        private final List<Object> objectsFired = new ArrayList<Object>();

        void registerNumberFired(@Observes @Extra Integer i) {
            objectsFired.add(i);
        }

        void registerSongFired(@Observes @Any Song s) {
            objectsFired.add(s);
        }

        void registerBroadcastFired(@Observes @Any Broadcast b) {
            objectsFired.add(b);
        }

        void registerArrayOfSongs(@Observes Song[] songs) {
            objectsFired.add(songs);
        }

        void registerArrayOfNumbers(@Observes Integer[] integers) {
            objectsFired.add(integers);
        }

        void registerArrayOfNumberPrimitives(@Observes int[] integers) {
            objectsFired.add(integers);
        }

        List<Object> getObjectsFired() {
            return objectsFired;
        }

        void reset() {
            objectsFired.clear();
        }
    }

    @Dependent
    static class TuneSelect<T> {
        @Inject
        @Any
        Event<Artist<T>> soloArtistEvent;

        @Inject
        @Any
        Event<Song> songEvent;

        @Inject
        @Any
        Event<Broadcast> broadcastEvent;

        void songPlaying(Song song) {
            songEvent.fire(song);
        }

        void broadcastPlaying(Broadcast broadcast) {
            broadcastEvent.fire(broadcast);
        }

        void soloArtistPlaying(Artist<T> soloArtist) {
            soloArtistEvent.fire(soloArtist);
        }
    }

    @Dependent
    static class EventTypeFamilyObserver {
        private static int objectEventQuantity;
        private static int generalEventQuantity;
        private static int abstractEventQuantity;
        private static int complexEventQuantity;

        void observeObject(@Observes Object event) {
            if (event instanceof ComplexEvent) {
                objectEventQuantity++;
            }
        }

        void observeGeneralEvent(@Observes GeneralEvent event) {
            generalEventQuantity++;
        }

        void observeAbstractEvent(@Observes AbstractEvent event) {
            abstractEventQuantity++;
        }

        void observeComplexEvent(@Observes ComplexEvent event) {
            complexEventQuantity++;
        }

        int getGeneralEventQuantity() {
            return generalEventQuantity;
        }

        int getAbstractEventQuantity() {
            return abstractEventQuantity;
        }

        int getComplexEventQuantity() {
            return complexEventQuantity;
        }

        int getObjectEventQuantity() {
            return objectEventQuantity;
        }

        int getTotalEventsObserved() {
            return objectEventQuantity + generalEventQuantity + abstractEventQuantity + complexEventQuantity;
        }

        void reset() {
            objectEventQuantity = 0;
            generalEventQuantity = 0;
            abstractEventQuantity = 0;
            complexEventQuantity = 0;
        }
    }

    static class Artist<T> {
    }

    static class Solo {
    }

    static class Song {
    }

    @Vetoed
    abstract static class Broadcast {
    }

    interface GeneralEvent {
    }

    @Vetoed
    abstract static class AbstractEvent implements GeneralEvent {
    }

    static class ComplexEvent extends AbstractEvent implements GeneralEvent {
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Qualifier
    public @interface Extra {
        class Literal extends AnnotationLiteral<Extra> implements Extra {
            private static final long serialVersionUID = 1L;
        }
    }
}
