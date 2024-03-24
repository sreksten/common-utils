package com.threeamigos.common.util.interfaces.persistence;

/**
 * The result of a load or save operation on an object.
 *
 * @author Stefano Reksten
 */
public interface PersistResult {

	/**
	 *
	 * @return true if the operation was successful, false otherwise.
	 */
	boolean isSuccessful();

	/**
	 * @return true if the object was not found during a load operation.
	 */
	boolean isNotFound();

	/**
	 * @return a human-readable error explaining what happened.
	 */
	String getError();

}