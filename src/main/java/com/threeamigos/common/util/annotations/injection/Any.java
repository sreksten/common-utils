package com.threeamigos.common.util.annotations.injection;

import com.threeamigos.common.util.interfaces.injection.Instance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify that any implementation of a given type should be injected.<br>
 * Used with {@link Instance}.
 *
 * @author Stefano Reksten
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Any {
}
