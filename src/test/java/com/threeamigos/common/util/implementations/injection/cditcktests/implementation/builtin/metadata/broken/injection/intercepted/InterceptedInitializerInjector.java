package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.injection.intercepted;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

@Dependent
public class InterceptedInitializerInjector {

    @Inject
    public void setIntercepted(@Intercepted Bean<InterceptedInitializerInjector> bean) {
    }
}
