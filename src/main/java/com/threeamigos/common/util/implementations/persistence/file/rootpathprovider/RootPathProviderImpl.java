package com.threeamigos.common.util.implementations.persistence.file.rootpathprovider;

import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ResourceBundle;

/**
 * An implementation of the {@link RootPathProvider} interface. Unless a root_path_directory System property
 * is specified, it uses the user.home System property as a starting point for the root path for the application.
 * It then adds the package name of the object or class passed as an argument and creates the directory structure if needed.
 */
public class RootPathProviderImpl implements RootPathProvider {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.file.rootpathprovider.RootPathProviderImpl.RootPathProviderImpl");
        }
        return bundle;
    }

    // End of static methods

    private final ExceptionHandler exceptionHandler;
    private String rootPath;
    private boolean rootPathAccessible;
    private boolean hasUnrecoverableErrors;

    /**
     * Builds a new RootPathProviderImpl instance. This is a class that determines a root path for the application in
     * which files are stored. Unless a root_path_directory System property is specified, it uses the user.home System
     * property as a starting point for the root path of the application, in which a subdirectory is created using the
     * package name of the object passed as an argument. The application's main class should be used.<br/>
     * If something goes wrong, the {@link #hasUnrecoverableErrors()} method returns true. In this case the program
     * should abort or somehow choose another path.
     * If the {@link #isRootPathAccessible()} method returns true the chosen path can be used to store files.
     *
     * @param object the object to use for root path determination
     * @param exceptionHandler the exception handler to use
     */
    public RootPathProviderImpl(final @NonNull Object object, final @NonNull ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        if (object == null) {
            throw new NullObjectException(getBundle().getString("nullObject"));
        }
        impl(object.getClass());
    }

    /**
     * Builds a new RootPathProviderImpl instance. This is a class that determines a root path for the application in
     * which files are stored. Unless a root_path_directory System property is specified, it uses the user.home System
     * property as a starting point for the root path of the application, in which a subdirectory is created using the
     * package name of the class passed as an argument. The application's main class should be used.<br/>
     * If something goes wrong, the {@link #hasUnrecoverableErrors()} method returns true. In this case the program
     * should abort or somehow choose another path.
     * If the {@link #isRootPathAccessible()} method returns true the chosen path can be used to store files.
     *
     * @param clazz the class to use for root path determination
     * @param exceptionHandler the exception handler to use
     */
    public RootPathProviderImpl(final @NonNull Class<?> clazz, final @NonNull ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        impl(clazz);
    }

    private void impl(final Class<?> clazz) {
        if (clazz == null) {
            throw new NullClassException(getBundle().getString("nullClass"));
        }
        if (exceptionHandler == null) {
            throw new NullExceptionHandlerException(getBundle().getString("nullExceptionHandler"));
        }
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
            try {
                rootPath = buildRootPath(preferencesPath, packageName);
                rootPathAccessible = true;
            } catch (DirectoryNotWriteableException e) {
                rootPathAccessible = false;
                hasUnrecoverableErrors = true;
            }
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

    /**
     * @return null if the root path is not accessible, otherwise the root path
     */
    public @Nullable String getRootPath() {
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

    private String extractPackageNameImpl(final Class<?> clazz) {
        String canonicalName = clazz.getCanonicalName();
        if (canonicalName == null) {
            throw new NoCanonicalNameException(getBundle().getString("noCanonicalName"));
        }
        Package classPackage = clazz.getPackage();
        if (classPackage == null) {
            throw new NoPackageException(getBundle().getString("noPackage"));
        }
        String packageName = classPackage.getName();
        if (packageName.isEmpty()) {
            throw new EmptyPackageException(getBundle().getString("emptyPackage"));
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

    private String getPreferencesPathImpl() {
        String home;
        String pathProperty = System.getProperty(ROOT_PATH_DIRECTORY_PARAMETER);
        if (pathProperty != null) {
            pathProperty = pathProperty.trim();
            if (pathProperty.isEmpty()) {
                throw new EmptyPathException(format("emptyPath", ROOT_PATH_DIRECTORY_PARAMETER));
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

    private void checkParentPathIsAccessibleImpl(final File file) {
        File parentDirectory = file.getParentFile();
        while (true) {
            if (parentDirectory.exists()) {
                if (!parentDirectory.canRead()) {
                    throw new ParentDirectoryNotReadableException(format("parentDirectoryNotReadable", parentDirectory.getAbsolutePath()));
                }
                if (!parentDirectory.canWrite()) {
                    throw new ParentDirectoryNotWriteableException(format("parentDirectoryNotWriteable", parentDirectory.getAbsolutePath()));
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

    private void checkFolderIsAccessibleImpl(final File file) {
        if (!file.isDirectory()) {
            throw new PathPointsToFileException(format("pathPointsToFile", file.getAbsolutePath()));
        }
        if (!file.canRead()) {
            throw new DirectoryNotReadableException(format("directoryNotReadable", file.getAbsolutePath()));
        }
        if (!file.canWrite()) {
            throw new DirectoryNotWriteableException(format("directoryNotWriteable", file.getAbsolutePath()));
        }
    }

    private String format(String key, String value) {
        return String.format(getBundle().getString(key), value);
    }

    private String buildRootPath(final String home, final String packageName) {
        String completePath = home + File.separator + "." + packageName;
        // Create directories if needed
        File file = new File(completePath);
        if (!file.exists()) {
            if (!new File(completePath).mkdirs()) {
                throw new DirectoryNotWriteableException(format("directoryNotWriteable", completePath));
            }
        }
        return completePath;
    }
}
