package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.ExceptionHandler;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ResourceBundle;

/**
 * An implementation of the {@link Persister} interface that uses a JSON file to
 * store and retrieve an entity. Uses {@link Json}. Specific
 * object type adapters can be added at will.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public class JsonFilePersister<T> extends AbstractFilePersister<T> implements Persister<T> {

    static final String FILENAME_EXTENSION = ".json";

    static ResourceBundle bundle;

    static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.file.JsonFilePersister.JsonFilePersister");
        }
        return bundle;
    }

    // End of static methods

    private final String filename;
    private final String entityDescription;
    private final Json<T> json;

    /**
     * @param filename          name of the file (without a path) used to store and
     *                          retrieve the entity
     * @param entityDescription a human-readable description of the entity
     * @param rootPathProvider  used to find the correct path for the entity
     * @param exceptionHandler  in case any problems arise
     */
    public JsonFilePersister(final @NonNull String filename, final @NonNull String entityDescription,
                             final @NonNull RootPathProvider rootPathProvider,
                             final @NonNull ExceptionHandler exceptionHandler, final @NonNull Json<T> json) {
        super(rootPathProvider, exceptionHandler);
        if (filename == null) {
            throw new IllegalArgumentException(getBundle().getString("noFilenameProvided"));
        }
        if (entityDescription == null) {
            throw new IllegalArgumentException(getBundle().getString("noEntityDescriptionProvided"));
        }
        if (json == null) {
            throw new IllegalArgumentException(getBundle().getString("noJsonProvided"));
        }
        this.filename = filename;
        this.entityDescription = entityDescription;
        this.json = json;
    }

    @Override
    protected void load(final @NonNull InputStream inputStream, final @NonNull T entity) throws IllegalArgumentException, IOException {
        json.fromJson(inputStream, entity);
    }

    @Override
    protected void save(final @NonNull OutputStream outputStream, final @NonNull T entity) throws IOException {
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
