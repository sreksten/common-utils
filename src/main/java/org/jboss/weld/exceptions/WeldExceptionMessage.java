package org.jboss.weld.exceptions;

/**
 * Minimal client-side compatibility contract used for Arquillian exception deserialization.
 */
public interface WeldExceptionMessage {

    String getAsString();
}

