package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.validation.ambiguous;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@Dependent
public class Producer {

    @Produces
    public Product produce() {
        return new Product();
    }

    public void dispose(@Disposes Product product, Animal animal) {
    }
}
