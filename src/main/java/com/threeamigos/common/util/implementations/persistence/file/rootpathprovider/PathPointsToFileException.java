package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class PathPointsToFileException extends IllegalArgumentException {

    public PathPointsToFileException(final String s) {
        super(String.format("%s is a file, not a folder.", s));
    }
}
