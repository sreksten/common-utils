package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class EmptyPackageException extends IllegalArgumentException {

    public EmptyPackageException() {
        super("A class with empty package name was passed to the RootPathProvider. Pass the main class and check it is contained in a proper package.");
    }

}
