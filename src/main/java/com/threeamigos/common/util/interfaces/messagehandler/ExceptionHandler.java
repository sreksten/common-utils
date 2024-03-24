package com.threeamigos.common.util.interfaces.messagehandler;

/**
 * An interface used to handle exceptions.
 * 
 * @author Stefano Reksten
 */
public interface ExceptionHandler {

	/**
	 * Handles a single exception.
	 * 
	 * @param exception an exception to handle
	 */
	void handleException(Exception exception);

}
