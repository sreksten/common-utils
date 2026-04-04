package com.threeamigos.common.util.implementations.injection.cditcktests.event.broken.observer.beanNotManaged;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverMethodOnIncorrectBeanTest {

    @Test
    void testObserverMethodNotOnManagedOrSessionBeanFails() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                AbstractBean.class,
                ConcreteBean.class,
                NonManagedBean.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            syringe.setup();
            assertTrue(syringe.getBeanManager().resolveObserverMethods(new String()).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    abstract static class AbstractBean {
        public abstract void observer(@Observes @Any String event);
    }

    static class ConcreteBean extends AbstractBean {
        @Override
        public void observer(String event) {
        }
    }

    public static class NonManagedBean {
        public NonManagedBean(String name) {
        }

        public void observe(@Observes @Any String event) {
        }
    }
}
