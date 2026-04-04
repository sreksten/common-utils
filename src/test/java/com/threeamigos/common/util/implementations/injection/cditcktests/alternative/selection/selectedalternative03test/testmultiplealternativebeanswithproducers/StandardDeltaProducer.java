package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative03test.testmultiplealternativebeanswithproducers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class StandardDeltaProducer {

    @Produces
    public Delta produce() {
        return new Delta("default");
    }
}
