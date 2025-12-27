package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.persistence.file.FilePersistResult;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * An implementation of the {@link FilePersistResult} interface.
 *
 * @author Stefano Reksten
 */
class FilePersistResultImpl implements FilePersistResult {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.file.FilePErsistResultImpl.FilePersistResultImpl");
        }
        return bundle;
    }

    public static FilePersistResultImpl notFound(final @NonNull String fileDescription) {
        if (fileDescription == null) {
            throw new IllegalArgumentException(getBundle().getString("nullFileDescriptionProvided"));
        }
        FilePersistResultImpl persistResult = new FilePersistResultImpl(String.format(getBundle().getString("noFileFound"), fileDescription));
        persistResult.notFound = true;
        return persistResult;
    }

    public static FilePersistResultImpl cannotBeRead(final @NonNull String fileDescription) {
        if (fileDescription == null) {
            throw new IllegalArgumentException(getBundle().getString("nullFileDescriptionProvided"));
        }
        return new FilePersistResultImpl(String.format(getBundle().getString("fileCannotBeRead"), fileDescription));
    }

    public static FilePersistResultImpl pathNotAccessible() {
        return new FilePersistResultImpl(getBundle().getString("directoryCannotBeAccessed"));
    }

    public static FilePersistResultImpl fileNotWriteable(final @NonNull String fileDescription) {
        if (fileDescription == null) {
            throw new IllegalArgumentException(getBundle().getString("nullFileDescriptionProvided"));
        }
        return new FilePersistResultImpl(String.format(getBundle().getString("fileCannotBeWritten"), fileDescription));
    }

    // End of static methods

    private final boolean successful;
    private boolean notFound;

    private String filename;

    private String error;

    FilePersistResultImpl() {
        successful = true;
    }

    FilePersistResultImpl(final @NonNull String error) {
        if (error == null) {
            throw new IllegalArgumentException(getBundle().getString("nullErrorProvided"));
        }
        successful = false;
        this.error = error;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public boolean isNotFound() {
        return notFound;
    }

    void setFilename(final @NonNull String filename) {
        if (filename == null) {
            throw new IllegalArgumentException(getBundle().getString("nullFilenameProvided"));
        }
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getError() {
        return error;
    }

}
