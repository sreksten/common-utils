package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * An abstract implementation of the {@link MessageHandler} interface that checks if a given message level is enabled
 * before forwarding the message to the concrete implementation. If the message parameter is null, an exception is
 * thrown. If the parameter is a Supplier, and the supplier returns null, an exception is thrown.<br/>
 * The advantage of using a Supplier is that if the message level is deactivated, the message construction can be
 * skipped, saving resources.
 *
 * @author Stefano Reksten
 */
public abstract class AbstractMessageHandler implements MessageHandler {

    private static ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.AbstractMessageHandler.AbstractMessageHandler");
        }
        return bundle;
    }

    // End of static methods

    private boolean isInfoEnabled = true;
    private boolean isWarnEnabled = true;
    private boolean isErrorEnabled = true;
    private boolean isDebugEnabled = true;
    private boolean isTraceEnabled = true;
    private boolean isExceptionEnabled = true;

    public boolean isInfoEnabled() {
        return isInfoEnabled;
    }

    public void setInfoEnabled(boolean infoEnabled) {
        isInfoEnabled = infoEnabled;
    }

    public boolean isWarnEnabled() {
        return isWarnEnabled;
    }

    public void setWarnEnabled(boolean warnEnabled) {
        isWarnEnabled = warnEnabled;
    }

    public boolean isErrorEnabled() {
        return isErrorEnabled;
    }

    public void setErrorEnabled(boolean errorEnabled) {
        isErrorEnabled = errorEnabled;
    }

    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        isDebugEnabled = debugEnabled;
    }

    public boolean isTraceEnabled() {
        return isTraceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        isTraceEnabled = traceEnabled;
    }

    public boolean isExceptionEnabled() {
        return isExceptionEnabled;
    }

    public void setExceptionEnabled(boolean exceptionEnabled) {
        isExceptionEnabled = exceptionEnabled;
    }

    public void handleInfoMessage(final @NonNull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleInfoMessage(messageSupplier.get());
    }

    @Override
    public void handleInfoMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isInfoEnabled) {
            handleInfoMessageImpl(message);
        }
    }

    protected abstract void handleInfoMessageImpl(@NonNull String message);

    public void handleWarnMessage(final @NonNull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleWarnMessage(messageSupplier.get());
    }

    @Override
    public void handleWarnMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isWarnEnabled) {
            handleWarnMessageImpl(message);
        }
    }

    protected abstract void handleWarnMessageImpl(@NonNull String message);

    public void handleErrorMessage(final @NonNull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleErrorMessage(messageSupplier.get());
    }

    @Override
    public void handleErrorMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isErrorEnabled) {
            handleErrorMessageImpl(message);
        }
    }

    protected abstract void handleErrorMessageImpl(@NonNull String message);

    public void handleDebugMessage(final @NonNull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleDebugMessage(messageSupplier.get());
    }

    @Override
    public void handleDebugMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isDebugEnabled) {
            handleDebugMessageImpl(message);
        }
    }

    protected abstract void handleDebugMessageImpl(@NonNull String message);

    @Override
    public void handleTraceMessage(final @NonNull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isTraceEnabled) {
            handleTraceMessageImpl(message);
        }
    }

    public void handleTraceMessage(final @NonNull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleTraceMessageImpl(messageSupplier.get());
    }

    protected abstract void handleTraceMessageImpl(@NonNull String message);

    @Override
    public void handleException(final @NonNull Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException(getBundle().getString("nullExceptionProvided"));
        }
        if (isExceptionEnabled) {
            handleExceptionImpl(exception);
        }
    }

    protected abstract void handleExceptionImpl(@NonNull Exception exception);

}
