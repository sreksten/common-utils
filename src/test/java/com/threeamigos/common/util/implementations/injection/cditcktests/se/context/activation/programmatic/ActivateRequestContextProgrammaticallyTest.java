package com.threeamigos.common.util.implementations.injection.cditcktests.se.context.activation.programmatic;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class ActivateRequestContextProgrammaticallyTest {

    @Test
    void programmaticRequestContextActivation() {
        try (SeContainer container = newInitializer().initialize()) {
            TestContextActivator contextActivator = container.select(TestContextActivator.class).get();
            boolean activated = contextActivator.activate();
            assertTrue(activated);
            assertFalse(contextActivator.activate());
            contextActivator.callRequestScopeBean();
            RequestScopeCounter counter = container.select(RequestScopeCounter.class).get();
            assertEquals(2, contextActivator.callRequestScopeBean());
            contextActivator.deactivate();

            contextActivator.activate();
            BeanManager beanManager = container.getBeanManager();
            assertTrue(beanManager.getContext(RequestScoped.class).isActive());
            assertEquals(1, contextActivator.callRequestScopeBean());
            contextActivator.deactivate();
            assertThrows(ContextNotActiveException.class, () -> beanManager.getContext(RequestScoped.class).isActive());
        }
    }

    @Test
    void requestControllerBuiltInBeanAvailable() {
        try (SeContainer container = newInitializer().initialize()) {
            BeanManager beanManager = container.getBeanManager();
            Bean<?> requestControllerBean = beanManager.resolve(beanManager.getBeans(RequestContextController.class));
            assertEquals(Dependent.class, requestControllerBean.getScope());
        }
    }

    @Test
    void requestControllerDeactivatedThrowsException() {
        try (SeContainer container = newInitializer().initialize()) {
            TestContextActivator contextActivator = container.select(TestContextActivator.class).get();
            assertThrows(ContextNotActiveException.class, contextActivator::deactivate);
        }
    }

    private SeContainerInitializer newInitializer() {
        return SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(TestContextActivator.class, RequestScopeCounter.class);
    }
}
