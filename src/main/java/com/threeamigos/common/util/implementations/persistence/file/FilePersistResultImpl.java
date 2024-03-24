package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.persistence.file.FilePersistResult;

/**
 * An implementation of the {@link FilePersistResult} interface.
 *
 * @author Stefano Reksten
 */
class FilePersistResultImpl implements FilePersistResult {

    public static FilePersistResultImpl notFound(final String fileDescription) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl("No " + fileDescription + " file found.");
        persistResult.notFound = true;
        return persistResult;
    }

    public static FilePersistResultImpl cannotBeRead(final String fileDescription) {
        return new FilePersistResultImpl(fileDescription + " cannot be read.");
    }

    public static FilePersistResultImpl pathNotAccessible() {
        return new FilePersistResultImpl("Directory cannot be accessed.");
    }

    public static FilePersistResultImpl fileNotWriteable(final String fileDescription) {
        return new FilePersistResultImpl(fileDescription + " cannot be written.");
    }

    private final boolean successful;
    private boolean notFound;

    private String filename;

    private String error;

    FilePersistResultImpl() {
        successful = true;
    }

    FilePersistResultImpl(final String error) {
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

    void setFilename(final String filename) {
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
