package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testalternativeproducerwithpriority;

public class Alpha {

    private final String value;

    public Alpha(String value) {
        this.value = value;
    }

    public String ping() {
        return value;
    }
}
