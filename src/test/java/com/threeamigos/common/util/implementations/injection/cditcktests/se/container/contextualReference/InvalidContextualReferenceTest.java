package com.threeamigos.common.util.implementations.injection.cditcktests.se.container.contextualReference;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Isolated
class InvalidContextualReferenceTest {

    @Test
    void testReferenceUsedAfterContainerShutdown() {
        SeContainerInitializer seContainerInitializer = SeContainerInitializer.newInstance();
        FooBean beanReferenceOutsideContainer;
        try (SeContainer seContainer = seContainerInitializer.disableDiscovery().addBeanClasses(FooBean.class).initialize()) {
            FooBean validBeanReference = seContainer.select(FooBean.class).get();
            validBeanReference.ping();
            beanReferenceOutsideContainer = validBeanReference;
        }

        FooBean reference = beanReferenceOutsideContainer;
        assertThrows(IllegalStateException.class, reference::ping);
    }
}
