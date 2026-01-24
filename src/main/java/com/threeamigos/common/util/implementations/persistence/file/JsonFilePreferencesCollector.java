package com.threeamigos.common.util.implementations.persistence.file;

import com.threeamigos.common.util.implementations.persistence.BasicPersistablesCollector;
import com.threeamigos.common.util.implementations.preferences.file.JsonFilePreferencesManager;
import com.threeamigos.common.util.interfaces.json.Json;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import com.threeamigos.common.util.interfaces.persistence.file.RootPathProvider;
import com.threeamigos.common.util.interfaces.preferences.Preferences;
import jakarta.annotation.Nonnull;

import java.util.ResourceBundle;

/**
 * A PersistablesCollector for Preferences stored in files in JSON format.
 *
 * @author Stefano Reksten
 */
public class JsonFilePreferencesCollector<T extends Preferences> extends BasicPersistablesCollector {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.persistence.file.JsonFilePreferencesCollector.JsonFilePreferencesCollector");
        }
        return bundle;
    }

    // End of static methods

    private final RootPathProvider rootPathProvider;
    private final MessageHandler messageHandler;
    private final StatusTrackerFactory<T> statusTrackerFactory;
    private final Json<T> json;

    /**
     * @param rootPathProvider to get the files directory
     * @param messageHandler   to inform the end user of loa and save operations
     *                         results
     */
    public JsonFilePreferencesCollector(final @Nonnull RootPathProvider rootPathProvider, final @Nonnull MessageHandler messageHandler,
                                        final @Nonnull StatusTrackerFactory<T> statusTrackerFactory, final @Nonnull Json<T> json) {
        super();
        if (rootPathProvider == null) {
            throw new IllegalArgumentException(getBundle().getString("noRootPathProviderProvided"));
        }
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("noMessageHandlerProvided"));
        }
        if (statusTrackerFactory == null) {
            throw new IllegalArgumentException(getBundle().getString("noStatusTrackerFactoryProvided"));
        }
        if (json == null) {
            throw new IllegalArgumentException(getBundle().getString("noJsonProvided"));
        }
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
    public void add(final @Nonnull T preferences, final @Nonnull String filename) {
        if (preferences == null) {
            throw new IllegalArgumentException(getBundle().getString("noPreferencesProvided"));
        }
        if (filename == null) {
            throw new IllegalArgumentException(getBundle().getString("noFilenameProvided"));
        }
        JsonFilePersister<T> persister = new JsonFilePersister<>(filename, preferences.getDescription(),
                rootPathProvider, messageHandler, json);
        add(new JsonFilePreferencesManager<>(preferences, statusTrackerFactory, persister, messageHandler));
    }

    /**
     * @param preferences a set of Preferences
     * @return true if this collector is tracking the given preferences
     */
    public boolean isTracking(final @Nonnull Preferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException(getBundle().getString("noPreferencesProvided"));
        }
        return getPersistables()
                .stream()
                .filter(p -> p instanceof JsonFilePreferencesManager<?>)
                .map(p -> (JsonFilePreferencesManager<?>) p)
                .anyMatch(p -> p.isTracking(preferences));
    }
}
