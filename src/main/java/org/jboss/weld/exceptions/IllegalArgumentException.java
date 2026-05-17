package org.jboss.weld.exceptions;

/**
 * Minimal client-side compatibility class for Arquillian to deserialize
 * server-side Weld IllegalArgumentException instances when running with
 * Syringe (which intentionally excludes Weld client dependencies).
 */
public class IllegalArgumentException extends java.lang.IllegalArgumentException {

    private static final long serialVersionUID = 2L;

    private final WeldExceptionMessage message;

    public IllegalArgumentException(Throwable throwable) {
        super(throwable);
        this.message = new WeldExceptionStringMessage(
                throwable != null ? throwable.getLocalizedMessage() : null);
    }

    public IllegalArgumentException(String message) {
        super();
        this.message = new WeldExceptionStringMessage(message);
    }

    public IllegalArgumentException(String message, Throwable throwable) {
        super(throwable);
        this.message = new WeldExceptionStringMessage(message);
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

    @Override
    public String getMessage() {
        return message != null ? message.getAsString() : super.getMessage();
    }
}
