package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class RegularBeanProducer {

    @Produces
    @ProducedByMethod
    Alpha produceAlphaByMethod() {
        return new Alpha("default");
    }

    @Produces
    @ProducedByField
    Alpha alphaByField = new Alpha("default");
}
