package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class DirectoryNotWriteableException extends IllegalArgumentException {

    public DirectoryNotWriteableException(final String localizedError) {
        super(localizedError);
    }
}
