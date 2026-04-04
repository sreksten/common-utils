package com.threeamigos.common.util.implementations.injection.cditcktests.event.fires.sync;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireSyncEventTest {

    @Test
    void testSyncObservesCalledInSameThread() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Probe.class, Helper.class,
                Letter.class, ParisPostOffice.class, PraguePostOffice.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            Probe probe = syringe.getBeanManager().createInstance().select(Probe.class).get();
            probe.fire();
            probe.helper.addThreadID((int) Thread.currentThread().getId());
            assertEquals(2, probe.helper.getCounter());
            assertEquals(1, probe.helper.getThreadIDs().size());
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class Probe {
        @Inject
        Helper helper;

        @Inject
        Event<Letter> event;

        void fire() {
            event.fire(new Letter());
        }
    }

    @ApplicationScoped
    public static class Helper {
        private final Set<Integer> threadIDs = new HashSet<Integer>();
        private int counter;

        public void addThreadID(Integer id) {
            threadIDs.add(id);
        }

        public Set<Integer> getThreadIDs() {
            return threadIDs;
        }

        public int getCounter() {
            return counter;
        }

        public void incrementCount() {
            counter++;
        }
    }

    static class Letter {
    }

    @Dependent
    static class ParisPostOffice {
        @Inject
        Helper helper;

        void observeLetter(@Observes Letter letter) {
            helper.addThreadID((int) Thread.currentThread().getId());
            helper.incrementCount();
        }
    }

    @Dependent
    static class PraguePostOffice {
        @Inject
        Helper helper;

        void observeLetter(@Observes Letter letter) {
            helper.addThreadID((int) Thread.currentThread().getId());
            helper.incrementCount();
        }
    }
}
