package com.threeamigos.common.util.interfaces.persistence.file;

/**
 * An interface used to determine the path to a directory (root path) containing all files related to an application.
 *
 * @author Stefano Reksten
 */
public interface RootPathProvider {

    /**
     * If an environment variable with this name exists, its value will be used as the root path.
     * Otherwise, the user's home directory will be used, in which a directory will be created using the
     * package name of the application's main class (if it doesn't already exist).
     */
    String ROOT_PATH_DIRECTORY_PARAMETER = "root_path_directory";

    /**
     * @return true if unrecoverable errors were found (any programming error, such as passing a class without package,
     * or any filesystem problem, such as a directory not existing, not readable, ...).
     * In this case the program should abort or somehow choose another path.
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
