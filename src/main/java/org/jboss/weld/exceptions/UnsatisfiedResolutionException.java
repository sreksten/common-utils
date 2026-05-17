package org.jboss.weld.exceptions;

/**
 * Minimal client-side compatibility class for Arquillian to deserialize
 * server-side Weld UnsatisfiedResolutionException instances.
 */
public class UnsatisfiedResolutionException extends jakarta.enterprise.inject.UnsatisfiedResolutionException {

    private static final long serialVersionUID = 2L;

    private final WeldExceptionMessage message;

    public UnsatisfiedResolutionException(Throwable throwable) {
        super(throwable);
        this.message = new WeldExceptionStringMessage(
                throwable != null ? throwable.getLocalizedMessage() : null);
    }

    public UnsatisfiedResolutionException(String message) {
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
