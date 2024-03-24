package com.threeamigos.common.util.interfaces.ui;

import java.awt.*;

/**
 * An interface that acts as a cache for Font objects.
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface FontService {

    public static final String STANDARD_FONT_NAME = "Serif";

    Font getFont(String fontName, int fontAttributes, int fontHeight);

}
