package com.threeamigos.common.util.interfaces.messagehandler;


import org.jspecify.annotations.NonNull;

/**
 * An interface used to handle trace messages.<br/>
 * If the message construction may be expensive, consider using a supplier instead
 * (see {@link SupplierTraceMessageHandler}).
 *
 * @author Stefano Reksten
 */
@FunctionalInterface
public interface TraceMessageHandler {

    /**
     * Handles a single trace message.
     *
     * @param traceMessage a trace message to show to the programmer
     */
    void handleTraceMessage(@NonNull String traceMessage);

}
