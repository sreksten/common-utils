package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NullExceptionHandlerException extends IllegalArgumentException {

    public NullExceptionHandlerException(final String localizedError) {
        super(localizedError);
    }
}
