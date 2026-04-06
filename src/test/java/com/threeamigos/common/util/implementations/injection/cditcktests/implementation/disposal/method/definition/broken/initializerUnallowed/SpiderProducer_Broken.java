package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.initializerUnallowed;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@Dependent
public class SpiderProducer_Broken {

    @Produces
    public static Spider getSpider() {
        return new Spider();
    }

    @Inject
    public static void destorySpider(@Disposes Spider spider) {
    }
}
