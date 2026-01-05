package com.threeamigos.common.util.annotations.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify that a specific implementation of a given type should be injected.
 *
 * @author Stefano Reksten
 */
@Target({ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Alternative {
    /**
     * The identifier for this specific alternative implementation.
     */
    String value();
}
