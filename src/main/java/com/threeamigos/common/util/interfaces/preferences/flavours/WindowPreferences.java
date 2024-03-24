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
	public void setWidth(int width);

	/**
	 * @return preferred window width
	 */
	public int getWidth();

	/**
	 * @param height preferred window height
	 */
	public void setHeight(int height);

	/**
	 * @return preferred window height
	 */
	public int getHeight();

	/**
	 * @param x preferred window x coordinate
	 */
	public void setX(int x);

	/**
	 * @return preferred window x coordinate
	 */
	public int getX();

	/**
	 * @param y preferred window y coordinate
	 */
	public void setY(int y);

	/**
	 * @return preferred window y coordinate
	 */
	public int getY();

}
