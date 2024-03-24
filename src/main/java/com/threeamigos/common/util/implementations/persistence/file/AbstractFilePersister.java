package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.PersistResult;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;

import java.io.*;

/**
 * An implementation of the {@link Persister} interface that uses a file strategy to store and retrieve entities.
 * Boilerplate code added to return a {@link PersistResult}, but the load and save methods for serializing and
 * deserializing the entity must be overridden.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public abstract class AbstractFilePersister<T> implements Persister<T> {

    protected final ExceptionHandler exceptionHandler;
    private final String rootPath;
    private final boolean rootPathAccessible;

    protected AbstractFilePersister(final RootPathProvider rootPathProvider, final ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        rootPath = rootPathProvider.getRootPath();
        rootPathAccessible = rootPathProvider.isRootPathAccessible();
    }

    /**
     * @return the complete filename with path to use to store this entity
     * @throws IllegalArgumentException if the name part contains the file separator character
     */
    public String getFilenameWithPath() throws IllegalArgumentException {
        String namePart = getNamePart();
        if (namePart.indexOf(File.separatorChar) >= 0) {
            throw new IllegalArgumentException("Name part must not contain the '" + File.separatorChar + "' character");
        }
        return rootPath + File.separatorChar + getNamePart();
    }

    /**
     * @return a human-readable description for this entity
     */
    protected abstract String getEntityDescription();

    /**
     * @return the filename (without path) to use for this entity
     */
    protected abstract String getNamePart();

    @Override
    public PersistResult load(final T entity) {
        if (rootPathAccessible) {
            String filename = getFilenameWithPath();
            File file = new File(filename);
            if (!file.exists()) {
                return FilePersistResultImpl.notFound(getEntityDescription());
            }
            if (!file.canRead()) {
                return FilePersistResultImpl.cannotBeRead(getEntityDescription());
            }

            try (InputStream inputStream = createInputStream(filename)) {
                load(inputStream, entity);
            } catch (Exception e) {
                return new FilePersistResultImpl(e.getMessage());
            }

            FilePersistResultImpl result = new FilePersistResultImpl();
            result.setFilename(filename);
            return result;
        } else {
            return FilePersistResultImpl.pathNotAccessible();
        }
    }

    /**
     * Override to simulate errors during tests
     *
     * @param filename source file
     * @return InputStream bound to the source file
     */
    protected InputStream createInputStream(final String filename) throws IOException {
        return new FileInputStream(filename);
    }

    protected abstract void load(final InputStream inputStream, final T entity) throws IOException, IllegalArgumentException;

    @Override
    public PersistResult save(final T entity) {
        if (rootPathAccessible) {
            String filename = getFilenameWithPath();
            File file = new File(filename);
            if (file.exists() && !file.canWrite()) {
                return FilePersistResultImpl.fileNotWriteable(getEntityDescription());
            }
            try (OutputStream outputStream = createOutputStream(filename)) {
                save(outputStream, entity);
            } catch (IOException e) {
                return new FilePersistResultImpl(e.getMessage());
            }
            FilePersistResultImpl result = new FilePersistResultImpl();
            result.setFilename(filename);
            return result;
        } else {
            return FilePersistResultImpl.pathNotAccessible();
        }
    }

    /**
     * Override to simulate errors during tests
     *
     * @param filename destination file
     * @return OutputStream bound to destination file
     */
    protected OutputStream createOutputStream(final String filename) throws IOException {
        return new FileOutputStream(filename);
    }

    protected abstract void save(final OutputStream outputStream, final T entity) throws IOException;

}
