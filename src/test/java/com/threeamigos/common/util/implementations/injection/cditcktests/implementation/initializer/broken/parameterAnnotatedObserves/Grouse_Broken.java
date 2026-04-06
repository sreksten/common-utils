package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.initializer.broken.parameterAnnotatedObserves;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@Dependent
public class Grouse_Broken {

    @Inject
    public void setName(String name, @Observes DangerCall dangerCall) {
    }
}
