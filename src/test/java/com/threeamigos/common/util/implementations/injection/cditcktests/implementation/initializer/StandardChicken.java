package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer;

import jakarta.enterprise.context.Dependent;

@StandardVariety
@Dependent
public class StandardChicken implements ChickenInterface {

    @Override
    public String getName() {
        return "Standard";
    }
}
