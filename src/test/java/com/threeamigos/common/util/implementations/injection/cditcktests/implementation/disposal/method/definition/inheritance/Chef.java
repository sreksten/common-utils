package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.inheritance;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@Dependent
public class Chef extends Cook {

    @Produces
    public Meal produceDefaultMeal() {
        return new Meal(this);
    }
}
