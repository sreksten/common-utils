package com.threeamigos.common.util.interfaces.persistence;

/**
 * An object whose state can be persisted.
 *
 * @author Stefano Reksten
 */
public interface Persistable {

	/**
	 * Ask the Persistable to save its state.
	 */
	void persist();

}
