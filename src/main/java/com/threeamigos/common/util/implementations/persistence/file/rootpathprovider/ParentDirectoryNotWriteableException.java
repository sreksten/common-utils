package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class ParentDirectoryNotWriteableException extends IllegalArgumentException {

    public ParentDirectoryNotWriteableException(final String s) {
        super(String.format("Folder parent %s cannot be written.", s));
    }
}
