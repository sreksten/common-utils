package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.interceptor;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD, PARAMETER, FIELD})
@Retention(RUNTIME)
@Qualifier
public @interface ProducedString {
}
