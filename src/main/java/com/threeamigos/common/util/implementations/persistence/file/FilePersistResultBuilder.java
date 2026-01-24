package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.persistence.file.FilePersistResult;
import jakarta.annotation.Nonnull;

public class FilePersistResultBuilder {

    private FilePersistResultBuilder() {
    }

    public static FilePersistResult successful(final @Nonnull String description, final @Nonnull String filename) {
        return new FilePersistResultImpl(description, filename);
    }

    public static FilePersistResult notFound(final @Nonnull String description, final @Nonnull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setNotFound();
        return persistResult;
    }

    public static FilePersistResult notReadable(final @Nonnull String description, final @Nonnull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setCannotBeRead();
        return persistResult;
    }

    public static FilePersistResult notWriteable(final @Nonnull String description, final @Nonnull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setFileNotWriteable();
        return persistResult;
    }

    public static FilePersistResult pathNotAccessible(final @Nonnull String description, final @Nonnull String filename) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setPathNotAccessible();
        return persistResult;
    }

    public static FilePersistResult error(final @Nonnull String description, final @Nonnull String filename, final @Nonnull String error) {
        FilePersistResultImpl persistResult = new FilePersistResultImpl(description, filename);
        persistResult.setError(error);
        return persistResult;
    }
}
