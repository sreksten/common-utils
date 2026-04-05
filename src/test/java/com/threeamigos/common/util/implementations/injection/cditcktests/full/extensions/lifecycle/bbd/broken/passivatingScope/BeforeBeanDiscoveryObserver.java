package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd.broken.passivatingScope;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

public class BeforeBeanDiscoveryObserver implements Extension {

    void observe(@Observes BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addScope(EpochScoped.class, false, true);
    }
}
