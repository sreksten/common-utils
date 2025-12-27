package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NoPackageException extends IllegalArgumentException {

    public NoPackageException(final String localizedError) {
        super(localizedError);
    }
}
