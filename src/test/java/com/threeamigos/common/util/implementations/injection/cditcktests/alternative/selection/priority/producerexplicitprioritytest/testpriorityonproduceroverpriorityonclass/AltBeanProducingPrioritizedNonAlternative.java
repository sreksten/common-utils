package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
@Alternative
@Priority(1)
public class AltBeanProducingPrioritizedNonAlternative {

    @Priority(20)
    @Produces
    @ProducedByMethod
    Delta produceDeltaByMethod() {
        return new Delta("alternative2");
    }

    @Priority(20)
    @Produces
    @ProducedByField
    Delta deltaByField = new Delta("alternative2");
}
