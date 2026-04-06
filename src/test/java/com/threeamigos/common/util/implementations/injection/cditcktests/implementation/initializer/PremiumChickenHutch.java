package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class PremiumChickenHutch {

    private ChickenInterface chicken;

    @Inject
    public void setChicken(@Preferred ChickenInterface chicken) {
        this.chicken = chicken;
    }

    public ChickenInterface getChicken() {
        return chicken;
    }
}
