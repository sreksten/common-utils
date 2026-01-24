package com.threeamigos.common.util.interfaces.ui;

import com.threeamigos.common.util.interfaces.PropertyChangeAware;
import jakarta.annotation.Nonnull;

/**
 * An interface that keeps track of user inputs (keyboard and mouse) and
 * notifies all registered listeners.
 *
 * @author Stefano Reksten
 */
public interface UserInputTracker extends PropertyChangeAware {

    @Nonnull InputConsumer getInputConsumer();

}
