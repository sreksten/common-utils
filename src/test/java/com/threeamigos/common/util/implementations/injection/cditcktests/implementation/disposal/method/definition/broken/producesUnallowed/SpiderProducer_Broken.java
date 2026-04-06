package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.producesUnallowed;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@Dependent
public class SpiderProducer_Broken {

    @Produces
    public static Spider getSpider() {
        return new Spider();
    }

    @Produces
    public static void destorySpider(@Disposes Spider spider) {
    }
}
