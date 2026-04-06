package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.inheritance;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@Dependent
public class AppleTree {

    @Produces
    @Yummy
    public Apple produceYummyApple() {
        return new Apple(this);
    }

    public void disposeApple(@Disposes @Any Apple apple) {
        Apple.disposedBy.add(this.getClass());
    }
}
