package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

public class Beta {

    private final String value;

    public Beta(String value) {
        this.value = value;
    }

    public String ping() {
        return value;
    }
}
