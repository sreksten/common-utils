package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class DirectoryNotWriteableException extends IllegalArgumentException {

    public DirectoryNotWriteableException(final String s) {
        super(String.format("Folder %s cannot be written.", s));
    }
}
