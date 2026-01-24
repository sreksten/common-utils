package com.threeamigos.common.util.interfaces.ui;

import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * An interface that acts as a cache for Font objects.
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface FontService {

    String STANDARD_FONT_NAME = "Serif";

    /**
     * Retrieves a Font object with the specified attributes.
     *
     * @param fontName name of the font
     * @param fontAttributes required attributes for the font
     * @param fontHeight height of the font
     * @return Font object with the specified attributes
     */
    Font getFont(@Nonnull String fontName, int fontAttributes, int fontHeight);

}
