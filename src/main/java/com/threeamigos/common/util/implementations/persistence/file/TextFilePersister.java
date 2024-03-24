package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * An abstract implementation of a text file persister. The two methods loadFromText and saveAsText must be overridden.
 *
 * @param <T> type of entity
 * @author Stefano Reksten
 */
public abstract class TextFilePersister<T> extends AbstractFilePersister<T> {

    /**
     * @param rootPathProvider to provide the root path where the entity should be persisted
     * @param exceptionHandler to inform the end user if any error arises
     */
    protected TextFilePersister(final RootPathProvider rootPathProvider, final ExceptionHandler exceptionHandler) {
        super(rootPathProvider, exceptionHandler);
    }

    /**
     * @param inputStream the stream from which data should be read
     * @param entity      the entity to be populated
     * @throws IllegalArgumentException if the file is mangled
     */
    @Override
    protected void load(final InputStream inputStream, final T entity) throws IOException, IllegalArgumentException {
        loadFromText(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)), entity);
    }

    /**
     * @param reader the reader used to fetch data
     * @param entity the entity to be populated
     */
    protected abstract void loadFromText(final BufferedReader reader, final T entity)
            throws IOException, IllegalArgumentException;

    /**
     * @param outputStream the stream to which a text-based representation of the entity should be saved
     * @param entity       the entity to persist
     */
    @Override
    protected void save(final OutputStream outputStream, final T entity) throws IOException {
        try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            saveAsText(printWriter, entity);
        }
    }

    /**
     * @param printWriter the PrintWriter to which a text-based representation of the entity should be saved
     * @param entity      the entity to persist
     * @throws IllegalArgumentException if for any reason the entity could not be saved
     */
    protected abstract void saveAsText(final PrintWriter printWriter, final T entity)
            throws IOException, IllegalArgumentException;

}
