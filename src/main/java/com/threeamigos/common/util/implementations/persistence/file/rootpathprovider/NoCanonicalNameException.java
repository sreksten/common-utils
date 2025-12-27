package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

public class NoCanonicalNameException extends IllegalArgumentException {
    
    public NoCanonicalNameException(final String localizedError) {
        super(localizedError);
    }

}
