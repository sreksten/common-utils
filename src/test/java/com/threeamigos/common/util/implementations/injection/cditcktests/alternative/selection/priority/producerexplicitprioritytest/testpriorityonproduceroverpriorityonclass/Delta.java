package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.priority.producerexplicitprioritytest.testpriorityonproduceroverpriorityonclass;

public class Delta {

    private final String value;

    public Delta(String value) {
        this.value = value;
    }

    public String ping() {
        return value;
    }
}
