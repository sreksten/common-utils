package com.threeamigos.common.util.implementations.ui;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import com.threeamigos.common.util.interfaces.ui.FontService;

/**
 * An implementation of the {@link FontService} interface.
 * 
 * @author Stefano Reksten
 */
public class FontServiceImpl implements FontService {

	private final Map<String, Font> fontMap = new HashMap<>();

	/**
	 * @param fontName name of desired font
	 * @param attributes desired font attributes
	 * @param fontHeight desired font height
	 * @return a Font
	 */
	@Override
	public Font getFont(String fontName, int attributes, int fontHeight) {
		String key = fontName + "-" + attributes + "-" + fontHeight;
		return fontMap.computeIfAbsent(key, h -> new Font(fontName, attributes, fontHeight));
	}

}
