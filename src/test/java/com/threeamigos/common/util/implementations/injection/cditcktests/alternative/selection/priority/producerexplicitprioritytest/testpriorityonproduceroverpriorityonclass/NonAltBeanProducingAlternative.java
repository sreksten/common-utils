package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class NonAltBeanProducingAlternative {

    @Alternative
    @Priority(10)
    @Produces
    @ProducedByMethod
    Beta produceBetaByMethod() {
        return new Beta("alternative");
    }

    @Alternative
    @Priority(10)
    @Produces
    @ProducedByField
    Beta betaByField = new Beta("alternative");

    @Alternative
    @Priority(10)
    @Produces
    @ProducedByMethod
    Gamma produceGammaByMethod() {
        return new Gamma("alternative");
    }

    @Alternative
    @Priority(10)
    @Produces
    @ProducedByField
    Gamma gammaByField = new Gamma("alternative");

    @Alternative
    @Priority(10)
    @Produces
    @ProducedByMethod
    Delta produceDeltaByMethod() {
        return new Delta("alternative");
    }

    @Alternative
    @Priority(10)
    @Produces
    @ProducedByField
    Delta deltaByField = new Delta("alternative");
}
