package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;

import java.io.File;

/**
 * An implementation of the {@link RootPathProvider} interface. Unless a root_path_directory System property
 * is specified, it uses the user.home System property as a starting point for the root path for the application.
 * It then adds the package name of the object passed as an argument and creates the directory structure if needed.
 */
public class RootPathProviderImpl implements RootPathProvider {

    protected ExceptionHandler exceptionHandler;
    private String rootPath;
    private boolean rootPathAccessible;
    private boolean hasUnrecoverableErrors;

    public RootPathProviderImpl(final Object object, final ExceptionHandler exceptionHandler) {
        if (object == null) {
            throw new NullObjectException();
        }
        impl(object.getClass(), exceptionHandler);
    }

    public RootPathProviderImpl(final Class<?> clazz, final ExceptionHandler exceptionHandler) {
        impl(clazz, exceptionHandler);
    }

    private void impl(final Class<?> clazz, final ExceptionHandler exceptionHandler) {
        if (clazz == null) {
            throw new NullClassException();
        }
        if (exceptionHandler == null) {
            throw new NullExceptionHandlerException();
        }
        this.exceptionHandler = exceptionHandler;
        String packageName = extractPackageName(clazz);
        if (packageName == null) {
            hasUnrecoverableErrors = true;
            return;
        }

        String preferencesPath = getPreferencesPath();
        if (preferencesPath != null) {
            File file = new File(preferencesPath);
            if (!file.exists()) {
                createNewDirectory(file, preferencesPath, packageName);
            } else {
                useExistingDirectory(file, preferencesPath, packageName);
            }
        }
    }

    private void createNewDirectory(final File file, final String preferencesPath, final String packageName) {
        if (checkParentPathIsAccessible(file)) {
            rootPath = buildRootPath(preferencesPath, packageName);
            rootPathAccessible = true;
        }
    }

    private void useExistingDirectory(final File file, final String preferencesPath, final String packageName) {
        if (checkFolderIsAccessible(file)) {
            rootPath = buildRootPath(preferencesPath, packageName);
            rootPathAccessible = true;
        }
    }

    public boolean hasUnrecoverableErrors() {
        return hasUnrecoverableErrors;
    }

    public boolean isRootPathAccessible() {
        return rootPathAccessible;
    }

    public String getRootPath() {
        return rootPath;
    }

    private String extractPackageName(final Class<?> clazz) {
        try {
            return extractPackageNameImpl(clazz);
        } catch (NoCanonicalNameException | NoPackageException | EmptyPackageException e) {
            exceptionHandler.handleException(e);
            return null;
        }
    }

    private static String extractPackageNameImpl(final Class<?> clazz) {
        String canonicalName = clazz.getCanonicalName();
        if (canonicalName == null) {
            throw new NoCanonicalNameException();
        }
        Package classPackage = clazz.getPackage();
        if (classPackage == null) {
            throw new NoPackageException();
        }
        String packageName = classPackage.getName();
        if (packageName.isEmpty()) {
            throw new EmptyPackageException();
        }
        return packageName;
    }

    private String getPreferencesPath() {
        try {
            return getPreferencesPathImpl();
        } catch (EmptyPathException e) {
            exceptionHandler.handleException(e);
            return null;
        }
    }

    private static String getPreferencesPathImpl() {
        String home;
        String pathProperty = System.getProperty(ROOT_PATH_DIRECTORY_PARAMETER);
        if (pathProperty != null) {
            pathProperty = pathProperty.trim();
            if (pathProperty.isEmpty()) {
                throw new EmptyPathException();
            } else {
                home = pathProperty;
            }
        } else {
            home = System.getProperty("user.home");
        }
        return home;
    }

    private boolean checkParentPathIsAccessible(final File file) {
        try {
            checkParentPathIsAccessibleImpl(file);
            return true;
        } catch (ParentDirectoryNotReadableException | ParentDirectoryNotWriteableException e) {
            exceptionHandler.handleException(e);
            return false;
        }
    }

    private static void checkParentPathIsAccessibleImpl(final File file) {
        File parentDirectory = file.getParentFile();
        while (true) {
            if (parentDirectory.exists()) {
                if (!parentDirectory.canRead()) {
                    throw new ParentDirectoryNotReadableException(parentDirectory.getAbsolutePath());
                }
                if (!parentDirectory.canWrite()) {
                    throw new ParentDirectoryNotWriteableException(parentDirectory.getAbsolutePath());
                }
                return;
            }
            parentDirectory = parentDirectory.getParentFile();
        }
    }

    private boolean checkFolderIsAccessible(final File file) {
        try {
            checkFolderIsAccessibleImpl(file);
            return true;
        } catch (PathPointsToFileException | DirectoryNotReadableException | DirectoryNotWriteableException e) {
            exceptionHandler.handleException(e);
            return false;
        }
    }

    private static void checkFolderIsAccessibleImpl(final File file) {
        if (!file.isDirectory()) {
            throw new PathPointsToFileException(file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new DirectoryNotReadableException(file.getAbsolutePath());
        }
        if (!file.canWrite()) {
            throw new DirectoryNotWriteableException(file.getAbsolutePath());
        }
    }

    private String buildRootPath(final String home, final String packageName) {
        String completePath = home + File.separator + "." + packageName;
        // Create directories if needed
        new File(completePath).mkdirs();
        return completePath;
    }
}
