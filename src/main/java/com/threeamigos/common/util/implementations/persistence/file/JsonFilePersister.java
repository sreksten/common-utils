package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An implementation of the {@link Persister} interface that uses a Json file to
 * store and retrieve an entity. Uses {@link com.google.gson.Gson}. Specific
 * object type adapters can be added at will.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public class JsonFilePersister<T> extends AbstractFilePersister<T> implements Persister<T> {

    static final String FILENAME_EXTENSION = ".json";

    private final String filename;
    private final String entityDescription;
    private final Json<T> json;

    /**
     * @param filename          name of the file (without path) used to store and
     *                          retrieve the entity
     * @param entityDescription a human-readable description of the entity
     * @param rootPathProvider  used to find the correct path for the entity
     * @param exceptionHandler  in case any problems arise
     */
    public JsonFilePersister(final String filename, final String entityDescription,
                             final RootPathProvider rootPathProvider,
                             final ExceptionHandler exceptionHandler, final Json<T> json) {
        super(rootPathProvider, exceptionHandler);
        this.filename = filename;
        this.entityDescription = entityDescription;
        this.json = json;
    }

    @Override
    protected void load(final InputStream inputStream, final T entity) throws IllegalArgumentException, IOException {
        json.fromJson(inputStream, entity);
    }

    @Override
    protected void save(final OutputStream outputStream, final T entity) throws IOException {
        json.toJson(entity, outputStream);
    }

    @Override
    protected String getEntityDescription() {
        return entityDescription;
    }

    @Override
    protected String getNamePart() {
        return filename + FILENAME_EXTENSION;
    }

}
