package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par55injection;

public class StaticProducedFieldPayload {

    private final String source;

    public StaticProducedFieldPayload(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
