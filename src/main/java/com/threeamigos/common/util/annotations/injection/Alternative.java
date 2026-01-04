package com.threeamigos.common.util.annotations.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Alternative {
    /**
     * The identifier for this specific alternative implementation.
     */
    String value();
}
