package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.broken.exception.discovery;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

public class BeanDiscoveryObserver implements Extension {

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        throw new FooException("This error should be treated as a definition error");
    }
}
