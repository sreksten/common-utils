package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class EmptyPathException extends IllegalArgumentException {

    public EmptyPathException(final String localizedError) {
        super(localizedError);
    }
}
