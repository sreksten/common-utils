package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An implementation of the {@link MessageHandler} interface that uses the
 * console to print info, warning, trace, and debug messages to System.out and errors and
 * exceptions to System.err.
 *
 * @author Stefano Reksten
 */
public class ConsoleMessageHandler extends AbstractMessageHandler {

    @Override
    protected void handleInfoMessageImpl(final String message) {
        System.out.println(format("INFO ", message)); //NOSONAR
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        System.out.println(format("WARN ", message)); //NOSONAR
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        System.err.println(format("ERROR", message)); //NOSONAR
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        System.out.println(format("DEBUG", message)); //NOSONAR
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        System.out.println(format("TRACE", message)); //NOSONAR
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        System.err.println(format("EXCEP", exception.getMessage()));
        exception.printStackTrace(System.err); //NOSONAR
    }

    private String format(String level, String message) {
        String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        return String.format("[%s] [%s] %s", date, level, message);
    }
}
