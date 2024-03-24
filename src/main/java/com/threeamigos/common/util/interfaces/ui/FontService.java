package com.threeamigos.common.util.interfaces.ui;

import java.awt.Font;

/**
 * An interface that acts as a cache for Font objects.
 *
 * @author Stefano Reksten
 */
public interface FontService {

	public static final String STANDARD_FONT_NAME = "Serif";

	Font getFont(String fontName, int fontAttributes, int fontHeight);

}
