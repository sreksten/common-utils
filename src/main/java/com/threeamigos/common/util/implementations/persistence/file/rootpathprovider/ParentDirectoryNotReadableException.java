package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class ParentDirectoryNotReadableException extends IllegalArgumentException {

    public ParentDirectoryNotReadableException(final String s) {
        super(String.format("Folder parent %s cannot be read.", s));
    }
}