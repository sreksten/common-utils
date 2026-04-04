package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative03test.testmultiplealternativebeanswithproducers;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
@Alternative
@Priority(200)
public class AlternativeDeltaProducer2 {

    @Produces
    public Delta produce() {
        return new Delta("alt2");
    }
}
