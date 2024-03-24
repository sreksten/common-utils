package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NoPackageException extends IllegalArgumentException {

    public NoPackageException() {
        super("A class with no package was passed to the RootPathProvider. This means you may be passing an array type, a primitive type or void. Pass the main class instead.");
    }
}
