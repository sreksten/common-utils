package com.threeamigos.common.util.implementations.injection.cditcktests.se.context.custom;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
class CustomRequestContextSETest {

    @Test
    void defineCustomRequestContext() {
        try (SeContainer seContainer = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(RequestScopeCounter.class, SecondCounter.class)
                .addExtensions(AfterBeanDiscoveryObserver.class)
                .initialize()) {
            RequestScopeCounter counter = seContainer.select(RequestScopeCounter.class).get();
            assertEquals(11, counter.increment());
        }
    }
}
