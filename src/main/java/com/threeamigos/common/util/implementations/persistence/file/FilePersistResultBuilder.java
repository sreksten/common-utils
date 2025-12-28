package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.persistence.file.FilePersistResult;
import org.jspecify.annotations.NonNull;

public class FilePersistResultBuilder {

    private FilePersistResultBuilder() {
    }

    public static FilePersistResult successful(final @NonNull String description, final @NonNull String filename) {
        return new FilePersistResultImpl(description, filename);
    }

    public static FilePersistResult notFound(final @NonNull String description, final @NonNull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setNotFound();
        return persistResult;
    }

    public static FilePersistResult notReadable(final @NonNull String description, final @NonNull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setCannotBeRead();
        return persistResult;
    }

    public static FilePersistResult notWriteable(final @NonNull String description, final @NonNull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setFileNotWriteable();
        return persistResult;
    }

    public static FilePersistResult pathNotAccessible(final @NonNull String description, final @NonNull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setPathNotAccessible();
        return persistResult;
    }

    public static FilePersistResult error(final @NonNull String description, final @NonNull String filename, final @NonNull String error) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setError(error);
        return persistResult;
    }
}
