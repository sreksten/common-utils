package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.methodAnnotatedProduces;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@Dependent
public class Pheasant_Broken {

    @Inject
    @Produces
    public void setName(String name) {
    }
}
