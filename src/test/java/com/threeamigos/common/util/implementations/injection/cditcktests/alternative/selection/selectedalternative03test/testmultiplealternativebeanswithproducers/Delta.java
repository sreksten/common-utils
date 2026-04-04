package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative03test.testmultiplealternativebeanswithproducers;

import jakarta.enterprise.inject.Vetoed;

@Vetoed
public class Delta {

    private final String value;

    public Delta(String value) {
        this.value = value;
    }

    public String ping() {
        return value;
    }
}
