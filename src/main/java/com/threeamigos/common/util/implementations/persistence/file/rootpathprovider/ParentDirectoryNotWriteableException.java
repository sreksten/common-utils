package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class ParentDirectoryNotWriteableException extends IllegalArgumentException {

    public ParentDirectoryNotWriteableException(final String localizedError) {
        super(localizedError);
    }
}
