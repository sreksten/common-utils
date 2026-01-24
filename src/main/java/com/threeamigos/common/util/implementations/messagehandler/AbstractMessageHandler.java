package com.threeamigos.common.util.implementations.messagehandler;

import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.annotation.Nonnull;

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

    private static volatile ResourceBundle bundle;

    private static ResourceBundle getBundle() {
        ResourceBundle local = bundle;
        if (local == null) {
            synchronized (AbstractMessageHandler.class) {
                local = bundle;
                if (local == null) {
                    local = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.messagehandler.AbstractMessageHandler.AbstractMessageHandler");
                    bundle = local;
                }
            }
        }
        return local;
    }

    // End of static methods

    private volatile boolean isInfoEnabled = true;
    private volatile boolean isWarnEnabled = true;
    private volatile boolean isErrorEnabled = true;
    private volatile boolean isDebugEnabled = true;
    private volatile boolean isTraceEnabled = true;
    private volatile boolean isExceptionEnabled = true;

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

    public void handleInfoMessage(final @Nonnull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleInfoMessage(messageSupplier.get());
    }

    @Override
    public void handleInfoMessage(final @Nonnull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isInfoEnabled) {
            handleInfoMessageImpl(message);
        }
    }

    protected abstract void handleInfoMessageImpl(final String message);

    public void handleWarnMessage(final @Nonnull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleWarnMessage(messageSupplier.get());
    }

    @Override
    public void handleWarnMessage(final @Nonnull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isWarnEnabled) {
            handleWarnMessageImpl(message);
        }
    }

    protected abstract void handleWarnMessageImpl(final String message);

    public void handleErrorMessage(final @Nonnull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleErrorMessage(messageSupplier.get());
    }

    @Override
    public void handleErrorMessage(final @Nonnull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isErrorEnabled) {
            handleErrorMessageImpl(message);
        }
    }

    protected abstract void handleErrorMessageImpl(final String message);

    public void handleDebugMessage(final @Nonnull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        handleDebugMessage(messageSupplier.get());
    }

    @Override
    public void handleDebugMessage(final @Nonnull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isDebugEnabled) {
            handleDebugMessageImpl(message);
        }
    }

    protected abstract void handleDebugMessageImpl(final String message);

    @Override
    public void handleTraceMessage(final @Nonnull String message) {
        if (message == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageProvided"));
        }
        if (isTraceEnabled) {
            handleTraceMessageImpl(message);
        }
    }

    public void handleTraceMessage(final @Nonnull Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            throw new IllegalArgumentException(getBundle().getString("nullMessageSupplierProvided"));
        }
        if (isTraceEnabled) {
            handleTraceMessage(messageSupplier.get());
        }
    }

    protected abstract void handleTraceMessageImpl(final String message);

    @Override
    public void handleException(final @Nonnull Exception exception) {
        if (exception == null) {
            throw new IllegalArgumentException(getBundle().getString("nullExceptionProvided"));
        }
        if (isExceptionEnabled) {
            handleExceptionImpl(exception);
        }
    }

    protected abstract void handleExceptionImpl(final Exception exception);

}
