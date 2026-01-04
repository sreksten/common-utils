package com.threeamigos.common.util.implementations.injection.exceptions;

public class ConcreteClassNotFoundException extends RuntimeException {

    public ConcreteClassNotFoundException(final String localizedError) {
        super(localizedError);
    }
}
