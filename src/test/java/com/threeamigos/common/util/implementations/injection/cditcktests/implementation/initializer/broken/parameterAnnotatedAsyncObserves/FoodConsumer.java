package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.parameterAnnotatedAsyncObserves;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

@Dependent
public class FoodConsumer {

    @Inject
    public void setFood(Food food, @ObservesAsync String message) {
    }
}
