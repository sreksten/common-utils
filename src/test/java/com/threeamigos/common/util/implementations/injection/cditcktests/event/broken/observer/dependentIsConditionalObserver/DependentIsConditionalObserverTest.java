package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.dependentIsConditionalObserver;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DependentIsConditionalObserverTest {

    @Test
    void testDependentBeanWithConditionalObserverMethodIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), AlarmSystem.class, BreakIn.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            assertThrows(DefinitionException.class, syringe::setup);
        } finally {
            syringe.shutdown();
        }
    }

    @Dependent
    static class AlarmSystem {
        void onBreakInAttempt(@Observes(notifyObserver = Reception.IF_EXISTS) BreakIn breakIn) {
            throw new AssertionError("Observer should not be invoked");
        }
    }

    static class BreakIn {
    }
}
