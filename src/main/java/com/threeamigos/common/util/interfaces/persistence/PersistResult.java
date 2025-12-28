package com.threeamigos.common.util.interfaces.persistence;

/**
 * The result of a load or save operation on an object.
 *
 * @author Stefano Reksten
 */
public interface PersistResult {

	/**
	 * @return a description of the object being persisted or loaded.
	 */
	String getDescription();

	/**
	 * @return true if the operation was successful, false otherwise.
	 */
	boolean isSuccessful();

	/**
	 * @return true if the object was not found during a load operation.
	 */
	boolean isNotFound();

	/**
	 * @return a localized string reporting that a problem occurred for file description
	 */
	String getProblemOccurredForFileDescription();

	/**
	 * @return a human-readable error explaining what happened.
	 */
	String getError();

	/**
	 * @return a machine-readable error code
	 */
	PersistResultReturnCodeEnum getReturnCode();

}