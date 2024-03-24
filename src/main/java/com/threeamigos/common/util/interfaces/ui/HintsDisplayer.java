package com.threeamigos.common.util.interfaces.ui;

import java.awt.*;

/**
 * An interface able to show Hints.
 *
 * @author Stefano Reksten
 */
public interface HintsDisplayer {

    /**
     * @param component parent component used to show hints (can be null)
     */
    public void showHints(Component component);

}
