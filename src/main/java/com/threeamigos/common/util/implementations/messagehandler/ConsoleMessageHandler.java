package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;

import java.io.PrintStream;
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

    private static final Object PRINT_LOCK = new Object();

    @Override
    protected void handleInfoMessageImpl(final String message) {
        print(System.out, format("INFO ", message));
    }

    @Override
    protected void handleWarnMessageImpl(final String message) {
        print(System.out, format("WARN ", message));
    }

    @Override
    protected void handleErrorMessageImpl(final String message) {
        print(System.err, format("ERROR", message));
    }

    @Override
    protected void handleDebugMessageImpl(final String message) {
        print(System.out, format("DEBUG", message));
    }

    @Override
    protected void handleTraceMessageImpl(final String message) {
        print(System.out, format("TRACE", message));
    }

    @Override
    protected void handleExceptionImpl(final Exception exception) {
        synchronized (PRINT_LOCK) {
            System.err.println(format("EXCEP", exception.getMessage()));
            exception.printStackTrace(System.err); //NOSONAR
        }
    }

    private String format(String level, String message) {
        String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return String.format("[%s] [%s] %s", date, level, message);
    }

    private void print(PrintStream stream, String formatted) {
        synchronized (PRINT_LOCK) {
            stream.println(formatted);
        }
    }
}
