package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.wildcardAndTypeVariable;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class ObserverMethodWithParametertizedTypeTest {

    @Test
    void testObserverMethodCanObserveTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            BostonTerrier.reset();
            Behavior event = new Behavior() {
            };
            syringe.getBeanManager().getEvent().select(Behavior.class).fire(event);
            assertTrue(BostonTerrier.observedTypeVariable);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodCanObserveWildcardType() {
        Syringe syringe = newSyringe();
        try {
            BostonTerrier.reset();
            List<Object> event = new ObjectList();
            syringe.getBeanManager().getEvent().select(new TypeLiteral<List<Object>>() {
            }).fire(event);
            assertTrue(BostonTerrier.observedWildcard);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodCanObserveArrayTypeVariable() {
        Syringe syringe = newSyringe();
        try {
            BostonTerrier.reset();
            Behavior[] event = new Behavior[]{};
            syringe.getBeanManager().getEvent().select(Behavior[].class).fire(event);
            assertTrue(BostonTerrier.observedArrayTypeVariable);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserverMethodCanObserveArrayWildcard() {
        Syringe syringe = newSyringe();
        try {
            BostonTerrier.reset();
            List<?>[] event = new ObjectList[0];
            syringe.getBeanManager().getEvent().select(new TypeLiteral<List<?>[]>() {
            }).fire(event);
            assertTrue(BostonTerrier.observedArrayWildCard);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Behavior.class,
                BostonTerrier.class,
                ObjectList.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    interface Behavior {
    }

    @Dependent
    static class BostonTerrier {
        static boolean observedTypeVariable;
        static boolean observedWildcard;
        static boolean observedArrayTypeVariable;
        static boolean observedArrayWildCard;

        public <T extends Behavior> void observesEventWithTypeParameter(@Observes T behavior) {
            observedTypeVariable = true;
        }

        public void observesEventTypeWithWildcard(@Observes List<?> someArray) {
            observedWildcard = true;
        }

        public <T extends Behavior> void observesArrayTypeVariable(@Observes T[] terriers) {
            observedArrayTypeVariable = true;
        }

        public void observesArrayWildcard(@Observes List<?>[] terriers) {
            observedArrayWildCard = true;
        }

        static void reset() {
            observedTypeVariable = false;
            observedWildcard = false;
            observedArrayTypeVariable = false;
            observedArrayWildCard = false;
        }
    }

    @SuppressWarnings("serial")
    static class ObjectList extends ArrayList<Object> {
    }
}
