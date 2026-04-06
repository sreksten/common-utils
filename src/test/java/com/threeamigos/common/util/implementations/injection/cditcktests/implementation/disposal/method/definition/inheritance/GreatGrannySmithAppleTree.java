package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.inheritance;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@Dependent
public class GreatGrannySmithAppleTree extends GrannySmithAppleTree {

    @Produces
    public Apple produceDefaultApple() {
        return new Apple(this);
    }
}
