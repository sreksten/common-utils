package com.threeamigos.common.util.implementations.injection.cditcktests.se.context.activation.interceptor;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class ActivateRequestContextByInterceptorTest {

    @Test
    void classInterceptorRequestContextActivation() {
        resetInterceptorState();
        SeContainerInitializer seContainerInitializer = newInitializer();
        try (SeContainer container = seContainerInitializer.initialize()) {
            ClassInterceptorContextActivator activator = container.select(ClassInterceptorContextActivator.class).get();
            assertEquals(11, activator.callRequestScopeBean());
            RequestContextObserver requestContextObserver = container.select(RequestContextObserver.class).get();
            assertEquals(1, requestContextObserver.getInitCounter());
            assertEquals(1, requestContextObserver.getBeforeDestroyedCounter());
            assertEquals(1, requestContextObserver.getDestroyedCounter());
        }
    }

    @Test
    void methodInterceptorRequestContextActivation() {
        resetInterceptorState();
        SeContainerInitializer seContainerInitializer = newInitializer();
        try (SeContainer container = seContainerInitializer.initialize()) {
            MethodInterceptorContextActivator activator = container.select(MethodInterceptorContextActivator.class).get();
            assertEquals(11, activator.callRequestScopeBean());
            RequestContextObserver requestContextObserver = container.select(RequestContextObserver.class).get();
            assertEquals(1, requestContextObserver.getInitCounter());
            assertEquals(1, requestContextObserver.getBeforeDestroyedCounter());
            assertEquals(1, requestContextObserver.getDestroyedCounter());
        }
    }

    @Test
    void builtInInterceptorHasGivenPriority() {
        resetInterceptorState();
        SeContainerInitializer seContainerInitializer = newInitializer();
        try (SeContainer container = seContainerInitializer.initialize()) {
            ClassInterceptorContextActivator activator = container.select(ClassInterceptorContextActivator.class).get();
            activator.callRequestScopeBean();
            assertFalse(BeforeActivationInterceptor.isRequestContextActive.get());
            assertTrue(AfterActivationInterceptor.isRequestContextActive.get());
        }
    }

    private SeContainerInitializer newInitializer() {
        return SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(
                        ClassInterceptorContextActivator.class,
                        MethodInterceptorContextActivator.class,
                        RequestScopeCounter.class,
                        SecondCounter.class,
                        RequestContextObserver.class,
                        BeforeActivationInterceptor.class,
                        AfterActivationInterceptor.class
                );
    }

    private void resetInterceptorState() {
        BeforeActivationInterceptor.isRequestContextActive.set(false);
        AfterActivationInterceptor.isRequestContextActive.set(false);
    }
}
