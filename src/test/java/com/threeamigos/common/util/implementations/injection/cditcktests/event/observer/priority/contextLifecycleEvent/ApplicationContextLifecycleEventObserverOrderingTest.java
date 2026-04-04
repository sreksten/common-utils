package com.threeamigos.common.util.implementations.injection.cditcktests.event.observer.priority.contextLifecycleEvent;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class ApplicationContextLifecycleEventObserverOrderingTest {

    @Test
    void testContextLifecycleEventOrdering() {
        ApplicationScopedObserver.reset();
        Syringe syringe = newSyringe();
        try {
            assertEquals("ABCD", ApplicationScopedObserver.getBuilder().toString());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), ApplicationScopedObserver.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @Dependent
    static class ApplicationScopedObserver {
        private static StringBuilder builder;

        static StringBuilder getBuilder() {
            if (builder == null) {
                builder = new StringBuilder();
            }
            return builder;
        }

        static void reset() {
            builder = new StringBuilder();
        }

        public void first(@Observes @Initialized(ApplicationScoped.class)
                          @Priority(Interceptor.Priority.APPLICATION - 100) Object obj) {
            getBuilder().append("A");
        }

        public void second(@Observes @Initialized(ApplicationScoped.class) Object obj) {
            getBuilder().append("B");
        }

        public void third(@Observes @Initialized(ApplicationScoped.class)
                          @Priority(Interceptor.Priority.APPLICATION + 600) Object obj) {
            getBuilder().append("C");
        }

        public void forth(@Observes @Initialized(ApplicationScoped.class)
                          @Priority(Interceptor.Priority.APPLICATION + 700) Object obj) {
            getBuilder().append("D");
        }
    }
}
