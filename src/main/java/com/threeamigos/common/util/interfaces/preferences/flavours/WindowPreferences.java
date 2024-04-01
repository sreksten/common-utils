package com.threeamigos.common.util.interfaces.preferences.flavours;

import com.threeamigos.common.util.interfaces.preferences.Preferences;

/**
 * An interface that keeps track of generic window preferences - position and dimension.
 *
 * @author Stefano Reksten
 */
public interface WindowPreferences extends Preferences {

    /**
     * @param width preferred window width
     */
    void setWidth(int width);

    /**
     * @return preferred window width
     */
    int getWidth();

    /**
     * @param height preferred window height
     */
    void setHeight(int height);

    /**
     * @return preferred window height
     */
    int getHeight();

    /**
     * @param x preferred window x coordinate
     */
    void setX(int x);

    /**
     * @return preferred window x coordinate
     */
    int getX();

    /**
     * @param y preferred window y coordinate
     */
    void setY(int y);

    /**
     * @return preferred window y coordinate
     */
    int getY();

}
