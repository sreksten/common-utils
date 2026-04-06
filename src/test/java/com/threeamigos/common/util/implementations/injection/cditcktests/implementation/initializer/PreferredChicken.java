package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer;

import jakarta.enterprise.context.Dependent;

@Preferred
@Dependent
public class PreferredChicken implements ChickenInterface {

    @Override
    public String getName() {
        return "Preferred";
    }
}
