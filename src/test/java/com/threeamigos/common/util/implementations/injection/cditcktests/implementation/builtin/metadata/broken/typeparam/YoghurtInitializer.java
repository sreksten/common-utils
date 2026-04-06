package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

@ApplicationScoped
public class YoghurtInitializer {

    @Inject
    public void setBean(Bean<Cream> bean) {
    }
}
