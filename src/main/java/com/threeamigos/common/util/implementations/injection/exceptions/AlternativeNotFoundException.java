package com.threeamigos.common.util.implementations.injection.exceptions;

public class AlternativeNotFoundException extends RuntimeException {

    public AlternativeNotFoundException(final String localizedError) {
        super(localizedError);
    }
}
