package com.threeamigos.common.util.implementations.preferences;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.interfaces.persistence.PersistResult;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.StatusTracker;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import com.threeamigos.common.util.interfaces.preferences.Preferences;
import com.threeamigos.common.util.interfaces.preferences.PreferencesManager;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A basic implementation of the {@link PreferencesManager} interface,
 * used to store and retrieve a set of {@link Preferences}. Provides basic functionality
 * keeping track of changes in preferences, but not the storing/retrieving method.
 * That part is delegated to the {@link Persister}.
 *
 * @param <T>
 * @author Stefano Reksten
 */
public class BasicPreferencesManager<T extends Preferences> implements PreferencesManager<T> {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.preferences.BasicPreferencesManager.BasicPreferencesManager");
        }
        return bundle;
    }

    // End of static methods

    static final String INVALID_PREFERENCES_TEMPLATE = "%s were invalid and have been replaced with default values. Error was: %s";

    private final T preferences;
    private final Persister<T> persister;
    private final StatusTracker<T> statusTracker;
    private final MessageHandler messageHandler;
    private boolean invalidAtLoad;

    /**
     * @param preferences          a set of {@link Preferences} to track
     * @param persister            a {@link Persister} that knows how to load and store the preferences
     * @param statusTrackerFactory an object that can produce a {@link StatusTracker} to observe changes
     *                             in the set of Preferences
     * @param messageHandler       a {@link MessageHandler} used if problems arise
     */
    public BasicPreferencesManager(final @Nonnull T preferences, final @Nonnull Persister<T> persister,
                                   final @Nonnull StatusTrackerFactory<T> statusTrackerFactory,
                                   final @Nonnull MessageHandler messageHandler) {
        if (preferences == null) {
            throw new IllegalArgumentException(getBundle().getString("noPreferencesProvided"));
        }
        if (persister == null) {
            throw new IllegalArgumentException(getBundle().getString("noPersisterProvided"));
        }
        if (statusTrackerFactory == null) {
            throw new IllegalArgumentException(getBundle().getString("noStatusTrackerFactoryProvided"));
        }
        if (messageHandler == null) {
            throw new IllegalArgumentException(getBundle().getString("noMessageHandlerProvided"));
        }
        this.preferences = preferences;
        this.statusTracker = statusTrackerFactory.buildStatusTracker(preferences);
        this.persister = persister;
        this.messageHandler = messageHandler;

        PersistResult persistResult = persister.load(preferences);
        if (persistResult.isSuccessful()) {
            try {
                preferences.validate();
            } catch (IllegalArgumentException e) {
                handleError(e.getMessage());
                preferences.loadDefaultValues();
                invalidAtLoad = true;
            }
        } else {
            if (!persistResult.isNotFound()) {
                handleError(persistResult.getError());
                invalidAtLoad = true;
            }
            preferences.loadDefaultValues();
        }
        statusTracker.loadInitialValues();
    }

    private void handleError(final String error) {
        messageHandler.handleErrorMessage(String.format(INVALID_PREFERENCES_TEMPLATE, preferences.getDescription(), error));
    }

    @Override
    public void persist() {
        if (invalidAtLoad || statusTracker.hasChanged()) {
            PersistResult persistResult = persister.save(preferences);
            if (!persistResult.isSuccessful()) {
                messageHandler.handleErrorMessage(persistResult.getError());
            }
        }
    }

    public boolean isTracking(final @Nonnull Preferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException(getBundle().getString("noPreferencesProvided"));
        }
        return Objects.equals(this.preferences, preferences);
    }
}
