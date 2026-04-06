package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.multiple;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@Dependent
public class BusFactory {

    @Produces
    public Bus producesBus() {
        return new Bus("School bus");
    }

    public void disposeBus(@Disposes Bus bus) {
    }

    public void disposeVehicle(@Disposes Vehicle bus) {
    }
}
