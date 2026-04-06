package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class ChickenHutch {

    public Fox fox;
    public Chicken chicken;

    @Inject
    public void setFox(Fox fox) {
        this.fox = fox;
    }

    @Inject
    public void setChicken(Chicken chicken) {
        this.chicken = chicken;
    }
}
