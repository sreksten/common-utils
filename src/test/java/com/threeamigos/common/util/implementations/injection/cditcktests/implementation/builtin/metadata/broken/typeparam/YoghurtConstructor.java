package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

@RequestScoped
public class YoghurtConstructor {

    public YoghurtConstructor() {
    }

    @Inject
    public YoghurtConstructor(Bean<Cream> bean) {
    }
}
