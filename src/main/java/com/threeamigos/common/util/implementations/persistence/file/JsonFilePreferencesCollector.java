package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.implementations.persistence.BasicPersistablesCollector;
import com.threeamigos.common.util.implementations.preferences.file.JsonFilePreferencesManager;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import com.threeamigos.common.util.interfaces.preferences.Preferences;

/**
 * A collector for preferences stored in files.
 *
 * @author Stefano Reksten
 */
public class JsonFilePreferencesCollector<T extends Preferences> extends BasicPersistablesCollector {

    private final RootPathProvider rootPathProvider;
    private final MessageHandler messageHandler;
    private final StatusTrackerFactory<T> statusTrackerFactory;
    private final Json<T> json;

    /**
     * @param rootPathProvider to get the files directory
     * @param messageHandler   to inform the end user of loa and save operations
     *                         results
     */
    public JsonFilePreferencesCollector(final RootPathProvider rootPathProvider, final MessageHandler messageHandler,
                                        final StatusTrackerFactory<T> statusTrackerFactory, final Json<T> json) {
        super();
        this.rootPathProvider = rootPathProvider;
        this.messageHandler = messageHandler;
        this.statusTrackerFactory = statusTrackerFactory;
        this.json = json;
    }

    /**
     * To keep track of a set of preferences
     *
     * @param preferences the preferences object
     * @param filename    the filename to use to save and retrieve those preferences
     */
    public void add(final T preferences, final String filename) {
        JsonFilePersister<T> persister = new JsonFilePersister<>(filename, preferences.getDescription(),
                rootPathProvider, messageHandler, json);
        add(new JsonFilePreferencesManager<>(preferences, statusTrackerFactory, persister, messageHandler));
    }

    public boolean isTracking(final Preferences preferences) {
        return getPersistables()
                .stream()
                .anyMatch(p -> p instanceof JsonFilePreferencesManager<?> manager && manager.isTracking(preferences));
    }
}
