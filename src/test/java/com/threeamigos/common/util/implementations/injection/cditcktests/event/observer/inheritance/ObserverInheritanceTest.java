package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.inheritance;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class ObserverInheritanceTest {

    @Test
    void testNonStaticObserverMethodInherited() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            syringe.getBeanManager().getEvent().select(Egg.class).fire(new Egg());

            assertEquals(2, ActionSequence.getSequenceSize());
            ActionSequence.assertSequenceDataContainsAll(Foo.class.getName(), Bar.class.getName());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Egg.class,
                Foo.class,
                Bar.class,
                Baz.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    static class Egg {
    }

    static abstract class AbstractEggObserver {
        public void observeNewEgg(@Observes Egg egg, BeanManager beanManager) {
            ActionSequence.addAction(this.getClass().getName());
        }
    }

    @Dependent
    static class Foo extends AbstractEggObserver {
    }

    @Dependent
    static class Bar extends Foo {
    }

    @Dependent
    static class Baz extends Bar {
        @Override
        public void observeNewEgg(Egg egg, BeanManager beanManager) {
            ActionSequence.addAction("blabla");
        }
    }

    static class ActionSequence {
        private static final List<String> SEQUENCE = new CopyOnWriteArrayList<String>();

        static void reset() {
            SEQUENCE.clear();
        }

        static void addAction(String action) {
            SEQUENCE.add(action);
        }

        static int getSequenceSize() {
            return SEQUENCE.size();
        }

        static void assertSequenceDataContainsAll(String... expected) {
            assertTrue(SEQUENCE.containsAll(Arrays.asList(expected)));
        }
    }
}
