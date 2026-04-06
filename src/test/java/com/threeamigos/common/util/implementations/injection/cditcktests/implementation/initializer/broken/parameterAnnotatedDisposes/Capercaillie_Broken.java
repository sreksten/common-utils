package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.parameterAnnotatedDisposes;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.inject.Inject;

@Dependent
public class Capercaillie_Broken {

    @Inject
    public void setName(String name, @Disposes ChickenHutch chickenHutch) {
    }
}
