package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

import static com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider.ROOT_PATH_DIRECTORY_PARAMETER;

public class EmptyPathException extends IllegalArgumentException {

    public EmptyPathException() {
        super("An empty path was supplied to the System property \"" + ROOT_PATH_DIRECTORY_PARAMETER + "\".");
    }
}
