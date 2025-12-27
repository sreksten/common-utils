package com.threeamigos.common.util.interfaces.messagehandler;

/**
 * An interface used to handle all notifications from an application.
 * 
 * @author Stefano Reksten
 */
public interface MessageHandler extends InfoMessageHandler, SupplierInfoMessageHandler,
        WarnMessageHandler, SupplierWarnMessageHandler,
        ErrorMessageHandler, SupplierErrorMessageHandler,
        DebugMessageHandler, SupplierDebugMessageHandler,
        TraceMessageHandler, SupplierTraceMessageHandler,
        ExceptionHandler {
}
