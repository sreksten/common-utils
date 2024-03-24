package com.threeamigos.common.util.interfaces.ui;

import java.awt.*;

/**
 * An interface able to show Hints.
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface HintsDisplayer {

    /**
     * @param component parent component used to show hints (can be null)
     */
    void showHints(Component component);

}
