package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class Probe {

    @Inject
    @ProducedByMethod
    private Beta betaMethodProducer;

    @Inject
    @ProducedByField
    private Beta betaFieldProducer;

    @Inject
    @ProducedByMethod
    private Gamma gammaMethodProducer;

    @Inject
    @ProducedByField
    private Gamma gammaFieldProducer;

    @Inject
    @ProducedByMethod
    private Delta deltaMethodProducer;

    @Inject
    @ProducedByField
    private Delta deltaFieldProducer;

    public Beta betaMethodProducer() {
        return betaMethodProducer;
    }

    public Beta betaFieldProducer() {
        return betaFieldProducer;
    }

    public Gamma gammaMethodProducer() {
        return gammaMethodProducer;
    }

    public Gamma gammaFieldProducer() {
        return gammaFieldProducer;
    }

    public Delta deltaMethodProducer() {
        return deltaMethodProducer;
    }

    public Delta deltaFieldProducer() {
        return deltaFieldProducer;
    }
}
