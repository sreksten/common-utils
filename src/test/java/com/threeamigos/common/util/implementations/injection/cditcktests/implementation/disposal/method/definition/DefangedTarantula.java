package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition;

public class DefangedTarantula extends Tarantula {

    private final int deaths;

    public DefangedTarantula(int deaths) {
        this.deaths = deaths;
    }

    @Override
    public int getDeathsCaused() {
        return deaths;
    }
}
