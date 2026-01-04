package com.threeamigos.common.util.implementations.injection.exceptions;

public class ImplementationNotFoundException extends RuntimeException {

    public ImplementationNotFoundException(final String localizedError) {
        super(localizedError);
    }
}
