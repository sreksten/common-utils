package com.threeamigos.common.util.implementations.injection.methods;

import javax.inject.Inject;

public class ClassWithMethodWithValidParameters {

    private FirstMethodParameter firstMethodParameter;
    private SecondMethodParameter secondMethodParameter;

    @Inject
    private void methodWithValidParameters(FirstMethodParameter firstMethodParameter,
                                           SecondMethodParameter secondMethodParameter) {
        this.firstMethodParameter = firstMethodParameter;
        this.secondMethodParameter = secondMethodParameter;
    }

    public FirstMethodParameter getFirstMethodParameter() {
        return firstMethodParameter;
    }

    public SecondMethodParameter getSecondMethodParameter() {
        return secondMethodParameter;
    }
}
