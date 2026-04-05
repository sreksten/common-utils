package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.addDefinitionError;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

public class AfterBeanDiscoveryObserver implements Extension {

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addDefinitionError(new AssertionError("This error should be treated as a definition error"));
    }
}
