package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NullExceptionHandlerException extends IllegalArgumentException {

    public NullExceptionHandlerException() {
        super("No ExceptionHandler was passed to the RootPathProvider.");
    }
}
