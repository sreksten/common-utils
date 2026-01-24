package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.persistence.PersistResultReturnCodeEnum;
import com.threeamigos.common.util.interfaces.persistence.file.FilePersistResult;
import jakarta.annotation.Nonnull;

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
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.file.FilePersistResultImpl.FilePersistResultImpl");
        }
        return bundle;
    }

    // End of static methods

    private final String fileDescription;
    private final String filename;
    private boolean successful;
    private boolean notFound;
    private String error;
    private PersistResultReturnCodeEnum returnCode = PersistResultReturnCodeEnum.SUCCESSFUL;

    FilePersistResultImpl(String fileDescription, String filename) {
        if (fileDescription == null) {
            throw new IllegalArgumentException(getBundle().getString("nullDescriptionProvided"));
        }
        if (filename == null) {
            throw new IllegalArgumentException(getBundle().getString("nullFilenameProvided"));
        }
        this.fileDescription = fileDescription;
        this.filename = filename;
        this.successful = true;
    }

    @Override
    public String getDescription() {
        return fileDescription;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public boolean isNotFound() {
        return notFound;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public String getProblemOccurredForFileDescription() {
        return String.format(getBundle().getString("problemOccurred"), fileDescription);
    }

    @Override
    public PersistResultReturnCodeEnum getReturnCode() {
        return returnCode;
    }

    void setNotFound() {
        this.successful = false;
        this.notFound = true;
        this.error = String.format(getBundle().getString("fileNotFound"), filename);
        returnCode = PersistResultReturnCodeEnum.NOT_FOUND;
    }

    void setCannotBeRead() {
        this.successful = false;
        this.error = String.format(getBundle().getString("fileCannotBeRead"), filename);
        returnCode = PersistResultReturnCodeEnum.CANNOT_BE_READ;
    }

    void setPathNotAccessible() {
        this.successful = false;
        this.error = String.format(getBundle().getString("directoryCannotBeAccessed"), filename);
        returnCode = PersistResultReturnCodeEnum.PATH_NOT_ACCESSIBLE;
    }

    void setFileNotWriteable() {
        this.successful = false;
        this.error = String.format(getBundle().getString("fileCannotBeWritten"), filename);
        returnCode = PersistResultReturnCodeEnum.CANNOT_BE_WRITTEN;
    }

    void setError(final @Nonnull String error) {
        if (error == null) {
            throw new IllegalArgumentException(getBundle().getString("nullErrorProvided"));
        }
        this.successful = false;
        this.error = error;
        returnCode = PersistResultReturnCodeEnum.ERROR;
    }

}
