package com.threeamigos.common.util.interfaces.messagehandler;


import jakarta.annotation.Nonnull;

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
    void handleTraceMessage(final @Nonnull String traceMessage);

}
