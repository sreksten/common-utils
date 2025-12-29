package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.Resolution;

/**
 * Standard resolutions
 */
public enum ResolutionEnum implements Resolution {

	FULL_ULTRA_HD("Full Ultra HD/8K", 7680, 4320),
	ULTRA_HD("Ultra HD/4K", 3840, 2160),
	QUAD_HD("Quad HD", 2560, 1440),
	FULL_HD("Full HD", 1920, 1080),
	SXGA("SXGA", 1280, 1024),
	HD("HD", 1280, 720),
	SD("SD", 640, 480);

	private final String name;
	private final int width;
	private final int height;

	ResolutionEnum(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
	}

	/**
	 * @return Resolution name (e.g. Quad HQ, SXGA...)
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return resolution width
	 */
	@Override
	public int getWidth() {
		return width;
	}

	/**
	 * @return resolution height
	 */
	@Override
	public int getHeight() {
		return height;
	}

	/**
	 * @return "Name (width x height)"
	 */
	@Override
	public String toString() {
		return String.format("%s (%d x %d)", name, width, height);
	}

}
