package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.validation.unsatisfied;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

import java.util.List;

@Dependent
public class Producer {

    @Produces
    public Product produce() {
        return new Product("Foo");
    }

    public void dispose(@Disposes Product product, List<Integer> integers) {
    }
}
