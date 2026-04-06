package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition;

import jakarta.enterprise.context.Dependent;

@Dependent
public class Tarantula extends Spider implements DeadlySpider {

    public int getDeathsCaused() {
        return 1;
    }
}
