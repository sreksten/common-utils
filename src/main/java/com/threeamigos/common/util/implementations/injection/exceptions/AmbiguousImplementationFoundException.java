package com.threeamigos.common.util.implementations.injection.exceptions;

public class AmbiguousImplementationFoundException extends RuntimeException {

    public AmbiguousImplementationFoundException(final String localizedError) {
        super(localizedError);
    }
}
