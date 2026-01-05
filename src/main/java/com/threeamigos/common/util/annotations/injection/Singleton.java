package com.threeamigos.common.util.annotations.injection;
import com.threeamigos.common.util.interfaces.injection.Injector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates to the dependency {@link Injector} that the specified class is a singleton.
 *
 * @author Stefano Reksten
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Singleton {
}
