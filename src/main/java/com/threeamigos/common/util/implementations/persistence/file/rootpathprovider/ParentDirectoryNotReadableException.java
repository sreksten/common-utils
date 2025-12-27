package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class ParentDirectoryNotReadableException extends IllegalArgumentException {

    public ParentDirectoryNotReadableException(final String localizedError) {
        super(localizedError);
    }
}