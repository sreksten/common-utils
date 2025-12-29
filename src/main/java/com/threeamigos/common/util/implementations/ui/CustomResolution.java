package com.threeamigos.common.util.implementations.ui;


import com.threeamigos.common.util.interfaces.ui.Resolution;

/**
 * A custom resolution.
 */
public class CustomResolution implements Resolution {

	private final int width;
	private final int height;

	public CustomResolution(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * @return "Custom (width x height)"
	 */
	@Override
	public String getName() {
		return toString();
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
	 * @return "Custom (width x height)"
	 */
	@Override
	public String toString() {
		return String.format("Custom (%d x %d)", width, height);
	}

}
