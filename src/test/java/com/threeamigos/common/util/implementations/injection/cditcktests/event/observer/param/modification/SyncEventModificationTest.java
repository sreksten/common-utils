package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.param.modification;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.ObserverMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class SyncEventModificationTest {

    @Test
    void testModifiedEventParameterIsPropagated() {
        Syringe syringe = newSyringe();
        try {
            CounterObserver02.count = 0;
            syringe.getBeanManager().getEvent().select(Counter.class).fire(new Counter());
            assertEquals(3, CounterObserver02.count);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Counter.class,
                CounterObserver01.class,
                CounterObserver02.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @Dependent
    static class Counter {
        private int i = 0;

        public int getI() {
            return i;
        }

        public void increment() {
            i++;
        }
    }

    @Dependent
    static class CounterObserver01 {
        public void observe(@Observes @Priority(ObserverMethod.DEFAULT_PRIORITY) Counter counter) {
            counter.increment();
        }
    }

    @Dependent
    static class CounterObserver02 {
        static int count;

        public void observe(@Observes @Priority(ObserverMethod.DEFAULT_PRIORITY + 100) Counter counter) {
            counter.increment();
        }

        public void observeNext(@Observes @Priority(ObserverMethod.DEFAULT_PRIORITY + 200) Counter counter) {
            counter.increment();
            count = counter.getI();
        }
    }
}
