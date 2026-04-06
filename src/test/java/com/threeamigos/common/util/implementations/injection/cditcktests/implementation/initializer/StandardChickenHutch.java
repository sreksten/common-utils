package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class StandardChickenHutch {

    private ChickenInterface chicken;

    @Inject
    public void setChicken(@StandardVariety ChickenInterface chicken) {
        this.chicken = chicken;
    }

    public ChickenInterface getChicken() {
        return chicken;
    }
}
