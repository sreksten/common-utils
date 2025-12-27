package com.threeamigos.common.util.interfaces.ui;

import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * An interface able to show Hints.
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface HintsDisplayer {

    /**
     * @param component the parent component used to show hints (can be null)
     */
    void showHints(@Nullable Component component);

}
