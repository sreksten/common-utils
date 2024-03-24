package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NullObjectException extends IllegalArgumentException {

    public NullObjectException() {
        super("No object was passed to the RootPathProvider.");
    }
}
