package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.generic;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import java.util.Collections;
import java.util.List;

@Dependent
public class Bar {

    @Produces
    public List<Integer> produceInts() {
        return Collections.emptyList();
    }
}
