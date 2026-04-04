package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.nonEmptyNamed.nonemptynamedtest.test;

import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Stereotype
@Target(TYPE)
@Retention(RUNTIME)
@Named("foo")
public @interface StereotypeWithNonEmptyNamed_Broken {
}
