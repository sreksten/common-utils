package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NoCanonicalNameException extends IllegalArgumentException {
    
    public NoCanonicalNameException() {
        super("A class with no canonical name was passed to the RootPathProvider. This means you are passing either a local, an anonymous or a hidden class. Pass the main class instead.");
    }

}
