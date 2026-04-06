package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.inheritance;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@Dependent
public class Cook {

    @Produces
    @Yummy
    public Meal produceYummyMeal() {
        return new Meal(this);
    }

    public void disposeMeal(@Disposes @Any Meal meal) {
        Meal.disposedBy.add(this.getClass());
    }
}
