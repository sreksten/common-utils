package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;

@Dependent
public class MilkDisposer {

    @Produces
    public Milk produceMilk() {
        return null;
    }

    public void disposes(@Disposes Milk milk, Bean<Cream> bean) {
    }
}
