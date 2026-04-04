package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class RegularBeanProducer {

    @Produces
    @ProducedByMethod
    Beta produceBetaByMethod() {
        return new Beta("default");
    }

    @Produces
    @ProducedByField
    Beta betaByField = new Beta("default");

    @Produces
    @ProducedByMethod
    Gamma produceGammaByMethod() {
        return new Gamma("default");
    }

    @Produces
    @ProducedByField
    Gamma gammaByField = new Gamma("default");

    @Produces
    @ProducedByMethod
    Delta produceDeltaByMethod() {
        return new Delta("default");
    }

    @Produces
    @ProducedByField
    Delta deltaByField = new Delta("default");
}
