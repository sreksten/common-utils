package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@Alternative
@Priority(1)
@ApplicationScoped
public class AltBeanProducingAlternative {

    @Alternative
    @Priority(20)
    @Produces
    @ProducedByMethod
    Beta produceBetaByMethod() {
        return new Beta("alternative2");
    }

    @Alternative
    @Priority(20)
    @Produces
    @ProducedByField
    Beta betaByField = new Beta("alternative2");
}
