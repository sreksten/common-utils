package com.threeamigos.common.util.implementations.preferences.file;

import com.threeamigos.common.util.implementations.preferences.BasicPreferencesManager;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import com.threeamigos.common.util.interfaces.persistence.Persister;
import com.threeamigos.common.util.interfaces.persistence.StatusTrackerFactory;
import com.threeamigos.common.util.interfaces.preferences.Preferences;
import com.threeamigos.common.util.interfaces.preferences.PreferencesManager;

/**
 * An implementation of the {@link PreferencesManager} that uses Json both as
 * file format and status tracker.
 *
 * @param <T> a set of {@link Preferences}
 * @author Stefano Reksten
 */
public class JsonFilePreferencesManager<T extends Preferences> extends BasicPreferencesManager<T> {

    /**
     * @param preferences          the set of preferences to track
     * @param statusTrackerFactory statusTrackerFactory able to handle all property
     *                             types of T
     * @param persister            Persister for a file of type T
     * @param messageHandler       a {@link MessageHandler} to be used if problems
     *                             arise.
     */
    public JsonFilePreferencesManager(final T preferences, final StatusTrackerFactory<T> statusTrackerFactory,
                                      final Persister<T> persister, final MessageHandler messageHandler) {
        super(preferences, persister, statusTrackerFactory, messageHandler);
    }
}
