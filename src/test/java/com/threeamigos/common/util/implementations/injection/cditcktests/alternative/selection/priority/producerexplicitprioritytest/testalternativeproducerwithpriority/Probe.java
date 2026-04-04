package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class Probe {

    @Inject
    @ProducedByMethod
    private Alpha alphaMethodProducer;

    @Inject
    @ProducedByField
    private Alpha alphaFieldProducer;

    public Alpha alphaMethodProducer() {
        return alphaMethodProducer;
    }

    public Alpha alphaFieldProducer() {
        return alphaFieldProducer;
    }
}
