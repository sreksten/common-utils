package org.jboss.weld.exceptions;

import java.io.Serializable;

/**
 * Minimal client-side compatibility implementation used for Arquillian exception deserialization.
 */
public class WeldExceptionStringMessage implements Serializable, WeldExceptionMessage {

    private static final long serialVersionUID = 2L;

    private String message;

    public WeldExceptionStringMessage(String message) {
        this.message = message;
    }

    @Override
    public String getAsString() {
        return message;
    }
}

