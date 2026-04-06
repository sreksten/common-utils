package com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

public class ModifyingExtension implements Extension {

    public void observeBeforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        event.addAnnotatedType(beanManager.createAnnotatedType(Gecko.class), buildId(Gecko.class));
        event.addAnnotatedType(beanManager.createAnnotatedType(Reptile.class), buildId(Reptile.class));
    }

    private static String buildId(Class<?> javaClass) {
        return ModifyingExtension.class.getName() + "_" + javaClass.getName();
    }
}
