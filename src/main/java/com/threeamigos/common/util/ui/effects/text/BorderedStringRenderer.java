package com.threeamigos.common.util.ui.effects.text;

import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A class that draws a string with a border around it.
 *
 * @author Stefano Reksten
 */
public class BorderedStringRenderer {

	private BorderedStringRenderer() {}

	public static void drawString(final @Nonnull Graphics2D graphics, final @Nonnull String s, final int x, final int y,
								  final @Nonnull Color borderColor, final @Nonnull Color messageColor) {
		Color previousColor = graphics.getColor();

		graphics.setColor(borderColor);
		for (int i = x - 1; i <= x + 1; i++) {
			for (int j = y - 1; j <= y + 1; j++) {
				if (i != x && j != y) {
					graphics.drawString(s, i, j);
				}
			}
		}
		graphics.setColor(messageColor);
		graphics.drawString(s, x, y);

		graphics.setColor(previousColor);
	}

}
