package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.abortProcessing.orderedObservers;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionInOrderedObserversAbortsProcessingTest {

    @Test
    void testOrderedObserversAbortedCorrectly() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Invitation.class, OrderedObservers.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            OrderedObservers.familyInvited = false;
            OrderedObservers.bestFriendsInvited = false;
            OrderedObservers.goodFriendsInvited = false;
            OrderedObservers.othersInvited = false;

            try {
                syringe.getBeanManager().getEvent().select(Invitation.class).fire(new Invitation());
            } catch (OrderedObservers.CancelledException e) {
                assertTrue(OrderedObservers.familyInvited);
                assertTrue(OrderedObservers.bestFriendsInvited);
                assertFalse(OrderedObservers.goodFriendsInvited);
                assertFalse(OrderedObservers.othersInvited);
            }
        } finally {
            syringe.shutdown();
        }
    }

    static class Invitation {
    }

    @Dependent
    static class OrderedObservers {
        static boolean bestFriendsInvited;
        static boolean familyInvited;
        static boolean goodFriendsInvited;
        static boolean othersInvited;

        void familyObserves(@Observes @Priority(Interceptor.Priority.APPLICATION - 500) Invitation invitation) {
            familyInvited = true;
        }

        void bestFriendsObserves(@Observes @Priority(Interceptor.Priority.APPLICATION - 400) Invitation invitation) {
            bestFriendsInvited = true;
            throw new CancelledException();
        }

        void goodFriendsObserves(@Observes @Priority(Interceptor.Priority.APPLICATION) Invitation invitation) {
            goodFriendsInvited = true;
        }

        void othersObserves(@Observes @Priority(Interceptor.Priority.APPLICATION + 100) Invitation invitation) {
            othersInvited = true;
        }

        class CancelledException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }
    }
}
