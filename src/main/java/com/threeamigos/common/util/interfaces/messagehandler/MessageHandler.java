package com.threeamigos.common.util.interfaces.messagehandler;

/**
 * An interface used to handle info, warn, error, debug, trace messages, and exceptions.
 * 
 * @author Stefano Reksten
 */
public interface MessageHandler extends InfoMessageHandler, WarnMessageHandler, ErrorMessageHandler,
        DebugMessageHandler, TraceMessageHandler, ExceptionHandler {

}
