package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.interceptor;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@Dependent
public class FooProducer {

    @Produces
    @ProducedString
    public String produce() {
        return "foo";
    }
}
