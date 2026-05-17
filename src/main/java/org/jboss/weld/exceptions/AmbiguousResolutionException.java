package org.jboss.weld.exceptions;

/**
 * Minimal client-side compatibility class for Arquillian to deserialize
 * server-side Weld AmbiguousResolutionException instances.
 */
public class AmbiguousResolutionException extends jakarta.enterprise.inject.AmbiguousResolutionException {

    private static final long serialVersionUID = 2L;

    private final WeldExceptionMessage message;

    public AmbiguousResolutionException(Throwable throwable) {
        super(throwable);
        this.message = new WeldExceptionStringMessage(
                throwable != null ? throwable.getLocalizedMessage() : null);
    }

    public AmbiguousResolutionException(String message) {
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
