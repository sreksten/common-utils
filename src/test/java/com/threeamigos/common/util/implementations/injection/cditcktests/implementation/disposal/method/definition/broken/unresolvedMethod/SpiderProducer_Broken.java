package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.unresolvedMethod;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;

@Dependent
public class SpiderProducer_Broken {

    @Produces
    public static Spider getSpider() {
        return new Spider();
    }

    public static void destorySpider(@Disposes Spider spider) {
    }

    public static void destorySpider2(@Disposes Cat cat) {
    }
}
