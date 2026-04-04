package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
@Priority(500)
public class NonAltBeanWithPrioProducingAlternative {

    @Produces
    @ProducedByMethod
    @Alternative
    Gamma produceGammaByMethod() {
        return new Gamma("alternative2");
    }

    @Produces
    @ProducedByField
    @Alternative
    Gamma gammaByField = new Gamma("alternative2");
}
