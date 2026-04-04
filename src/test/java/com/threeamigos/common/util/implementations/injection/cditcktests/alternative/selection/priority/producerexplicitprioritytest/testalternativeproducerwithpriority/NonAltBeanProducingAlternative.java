package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority;

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
    Alpha produceAlphaByMethod() {
        return new Alpha("alternative");
    }

    @Alternative
    @Priority(10)
    @Produces
    @ProducedByField
    Alpha alphaByField = new Alpha("alternative");
}
