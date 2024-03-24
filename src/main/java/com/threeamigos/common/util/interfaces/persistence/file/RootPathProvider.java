package com.threeamigos.common.util.interfaces.persistence.file;

/**
 * An interface used to determine the path to a directory (root path) containing all files related to this application.
 *
 * @author Stefano Reksten
 */
public interface RootPathProvider {

    String ROOT_PATH_DIRECTORY_PARAMETER = "root_path_directory";

    /**
     * @return true if unrecoverable errors were found (directory not existing, not readable, ...).
     * In this case the program should abort, or somehow choose another path.
     */
    boolean hasUnrecoverableErrors();

    /**
     * @return true if the user has permission to read or write in the root path
     */
    boolean isRootPathAccessible();

    /**
     * @return the path to a directory (root path) for this application's files.
     */
    String getRootPath();

}
