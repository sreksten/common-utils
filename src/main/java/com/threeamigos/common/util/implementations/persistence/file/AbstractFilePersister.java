package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.PersistResult;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.jspecify.annotations.NonNull;

import java.io.*;
import java.util.ResourceBundle;

/**
 * An implementation of the {@link Persister} interface that uses a file strategy to store and retrieve entities.
 * Boilerplate code added to return a {@link PersistResult}, but the load and save methods for serializing and
 * deserializing the entity must be overridden.
 *
 * @param <T> type of entity
 * @author Stefano Reksten
 */
public abstract class AbstractFilePersister<T> implements Persister<T> {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.file.AbstractFilePersister.AbstractFilePersister");
        }
        return bundle;
    }

    // End of static methods

    protected final ExceptionHandler exceptionHandler;
    private final String rootPath;
    private final boolean rootPathAccessible;

    /**
     * @param rootPathProvider to know where to store the entity
     * @param exceptionHandler to inform the end user if any error arises
     */
    protected AbstractFilePersister(final @NonNull RootPathProvider rootPathProvider, final @NonNull ExceptionHandler exceptionHandler) {
        if (rootPathProvider == null) {
            throw new IllegalArgumentException(getBundle().getString("noRootPathProviderProvided"));
        }
        if (exceptionHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("noExceptionHandlerProvided"));
        }
        this.exceptionHandler = exceptionHandler;
        rootPath = rootPathProvider.getRootPath();
        rootPathAccessible = rootPathProvider.isRootPathAccessible();
    }

    /**
     * @return the complete filename with a path to use to store this entity
     * @throws IllegalArgumentException if the name part contains the file separator character
     */
    public String getFilenameWithPath() throws IllegalArgumentException {
        String namePart = getNamePart();
        if (namePart.indexOf(File.separatorChar) >= 0) {
            throw new IllegalArgumentException(String.format(getBundle().getString("mustNotContainFileSeparator"), File.separator));
        }
        return rootPath + File.separator + getNamePart();
    }

    /**
     * @return a human-readable description for this entity
     */
    protected abstract String getEntityDescription();

    /**
     * @return the filename (without a path) to use for this entity
     */
    protected abstract String getNamePart();

    @Override
    public PersistResult load(final @NonNull T entity) {
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        String entityDescription = getEntityDescription();
        String filename = getFilenameWithPath();
        if (rootPathAccessible) {
            File file = new File(filename);
            if (!file.exists()) {
                return FilePersistResultBuilder.notFound(entityDescription, filename);
            }
            if (!file.canRead()) {
                return FilePersistResultBuilder.notReadable(entityDescription, filename);
            }

            try (InputStream inputStream = createInputStream(filename)) {
                load(inputStream, entity);
            } catch (Exception e) {
                return FilePersistResultBuilder.error(entityDescription, filename, e.getMessage());
            }

            return FilePersistResultBuilder.successful(entityDescription, filename);
        } else {
            return FilePersistResultBuilder.pathNotAccessible(entityDescription, filename);
        }
    }

    /**
     * Override to simulate errors during tests
     *
     * @param filename source file
     * @return InputStream bound to the source file
     */
    protected InputStream createInputStream(final @NonNull String filename) throws IOException {
        return new FileInputStream(filename);
    }

    protected abstract void load(final @NonNull InputStream inputStream, final @NonNull T entity) throws IOException, IllegalArgumentException;

    @Override
    public PersistResult save(final @NonNull T entity) {
        if (entity == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityProvided"));
        }
        String entityDescription = getEntityDescription();
        String filename = getFilenameWithPath();
        if (rootPathAccessible) {
            File file = new File(filename);
            if (file.exists() && !file.canWrite()) {
                return FilePersistResultBuilder.notWriteable(entityDescription, filename);
            }
            try (OutputStream outputStream = createOutputStream(filename)) {
                save(outputStream, entity);
            } catch (IOException e) {
                return FilePersistResultBuilder.error(entityDescription, filename, e.getMessage());
            }
            return FilePersistResultBuilder.successful(entityDescription, filename);
        } else {
            return FilePersistResultBuilder.pathNotAccessible(entityDescription, filename);
        }
    }

    /**
     * Override to simulate errors during tests
     *
     * @param filename destination file
     * @return OutputStream bound to a destination file
     */
    protected OutputStream createOutputStream(final @NonNull String filename) throws IOException {
        return new FileOutputStream(filename);
    }

    protected abstract void save(final @NonNull OutputStream outputStream, final @NonNull T entity) throws IOException;

}
