package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;

@Dependent
public class MilkProducer {

    @Produces
    public Milk produceMilk(Bean<Cream> bean) {
        return null;
    }
}
