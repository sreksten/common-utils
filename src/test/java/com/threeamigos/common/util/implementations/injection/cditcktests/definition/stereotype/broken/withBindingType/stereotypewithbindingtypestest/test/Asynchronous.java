package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.withBindingType.stereotypewithbindingtypestest.test;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, METHOD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface Asynchronous {
}
