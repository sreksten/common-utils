package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

public class Gamma {

    private final String value;

    public Gamma(String value) {
        this.value = value;
    }

    public String ping() {
        return value;
    }
}
