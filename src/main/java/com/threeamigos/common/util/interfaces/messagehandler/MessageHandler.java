package com.threeamigos.common.util.interfaces.messagehandler;

/**
 * An interface used to handle info, warn or error messages, and exceptions.
 * 
 * @author Stefano Reksten
 */
public interface MessageHandler extends InfoMessageHandler, WarnMessageHandler, ErrorMessageHandler, ExceptionHandler {

}
