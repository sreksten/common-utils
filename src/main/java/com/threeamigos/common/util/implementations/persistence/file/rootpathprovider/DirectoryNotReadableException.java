package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class DirectoryNotReadableException extends IllegalArgumentException {

    public DirectoryNotReadableException(final String localizedError) {
        super(localizedError);
    }
}
