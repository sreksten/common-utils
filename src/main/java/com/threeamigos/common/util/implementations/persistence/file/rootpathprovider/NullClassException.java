package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NullClassException extends IllegalArgumentException {

    public NullClassException() {
        super("No class was passed to the RootPathProvider.");
    }
}
