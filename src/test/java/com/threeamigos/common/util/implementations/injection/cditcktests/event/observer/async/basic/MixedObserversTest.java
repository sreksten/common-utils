package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.async.basic;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
// Legacy CDI 2.0-era TCK parity: this fixture uses @Priority on an @ObservesAsync event parameter.
// In CDI 4.1 this is non-portable and rejected by default (see cdi41 ObserverNotificationsTest).
class MixedObserversTest {

    @Test
    void testSyncEventIsDeliveredOnlyToSyncObservers() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            MassachusettsInstituteObserver.threadId.set(0);
            OxfordUniversityObserver.threadId.set(0);
            StandfordUniversityObserver.threadId.set(0);

            syringe.getBeanManager().getEvent().select(ScientificExperiment.class).fire(new ScientificExperiment());

            ActionSequence.assertSequenceDataEquals(OxfordUniversityObserver.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testQualifiedAsyncEventIsDeliveredOnlyToAsyncObservers() throws Exception {
        Syringe syringe = newSyringe();
        try {
            BlockingQueue<Experiment> queue = new LinkedBlockingQueue<Experiment>();

            syringe.getBeanManager().getEvent()
                    .select(ScientificExperiment.class, AmericanLiteral.INSTANCE)
                    .fireAsync(new ScientificExperiment())
                    .thenAccept(queue::offer);

            Experiment experiment = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(experiment);
            assertEquals(3, experiment.getUniversities().size());
            assertTrue(experiment.getUniversities().contains(YaleUniversityObserver.class));
            assertTrue(experiment.getUniversities().contains(StandfordUniversityObserver.class));
            assertTrue(experiment.getUniversities().contains(MassachusettsInstituteObserver.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAsyncObserversCalledInDifferentThread() throws Exception {
        Syringe syringe = newSyringe();
        try {
            BlockingQueue<Experiment> queue = new LinkedBlockingQueue<Experiment>();
            int threadId = (int) Thread.currentThread().getId();
            MassachusettsInstituteObserver.threadId.set(0);

            syringe.getBeanManager().getEvent()
                    .select(ScientificExperiment.class)
                    .fireAsync(new ScientificExperiment())
                    .thenAccept(queue::offer);

            Experiment experiment = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(experiment);
            assertEquals(2, experiment.getUniversities().size());
            assertTrue(experiment.getUniversities().contains(StandfordUniversityObserver.class));
            assertTrue(experiment.getUniversities().contains(MassachusettsInstituteObserver.class));
            assertNotEquals(threadId, MassachusettsInstituteObserver.threadId.get());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                American.class,
                AmericanLiteral.class,
                Experiment.class,
                ScientificExperiment.class,
                MixedObservers.class,
                MassachusettsInstituteObserver.class,
                OxfordUniversityObserver.class,
                YaleUniversityObserver.class,
                StandfordUniversityObserver.class,
                ActionSequence.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        // Compatibility escape hatch for this outdated/non-portable declaration only.
        syringe.allowNonPortableAsyncObserverEventParameterPriority(true);
        syringe.setup();
        return syringe;
    }

    @Target({TYPE, METHOD, PARAMETER, FIELD})
    @Retention(RUNTIME)
    @Qualifier
    public @interface American {
    }

    public static class AmericanLiteral extends AnnotationLiteral<American> implements American {
        private static final long serialVersionUID = 1L;
        static final American INSTANCE = new AmericanLiteral();
    }

    static class Experiment {
        private final List<Class<?>> universities = new CopyOnWriteArrayList<Class<?>>();

        List<Class<?>> getUniversities() {
            return universities;
        }

        void addUniversity(Class<?> university) {
            universities.add(university);
        }
    }

    static class ScientificExperiment extends Experiment {
    }

    static class MixedObservers {
    }

    @Dependent
    static class MassachusettsInstituteObserver {
        static final AtomicInteger threadId = new AtomicInteger();

        void observes(@ObservesAsync @Priority(2000) Experiment experiment) {
            experiment.addUniversity(getClass());
            ActionSequence.addAction(getClass().getSimpleName());
            threadId.set((int) Thread.currentThread().getId());
        }
    }

    @Dependent
    static class OxfordUniversityObserver {
        static final AtomicInteger threadId = new AtomicInteger();

        void observes(@Observes Experiment experiment) {
            experiment.addUniversity(getClass());
            ActionSequence.addAction(getClass().getSimpleName());
            threadId.set((int) Thread.currentThread().getId());
        }
    }

    @Dependent
    static class YaleUniversityObserver {
        void observes(@ObservesAsync @American Experiment experiment) {
            experiment.addUniversity(getClass());
            ActionSequence.addAction(getClass().getSimpleName());
        }
    }

    @Dependent
    static class StandfordUniversityObserver {
        static final AtomicInteger threadId = new AtomicInteger();

        void observes(@ObservesAsync Experiment experiment) {
            experiment.addUniversity(getClass());
            ActionSequence.addAction(getClass().getSimpleName());
            threadId.set((int) Thread.currentThread().getId());
        }
    }

    static class ActionSequence {
        private static final List<String> actions = new CopyOnWriteArrayList<String>();

        static void reset() {
            actions.clear();
        }

        static void addAction(String action) {
            actions.add(action);
        }

        static void assertSequenceDataEquals(Class<?>... expectedClasses) {
            List<String> expected = new ArrayList<String>();
            for (Class<?> expectedClass : expectedClasses) {
                expected.add(expectedClass.getSimpleName());
            }
            assertEquals(expected, new ArrayList<String>(actions));
        }
    }
}
