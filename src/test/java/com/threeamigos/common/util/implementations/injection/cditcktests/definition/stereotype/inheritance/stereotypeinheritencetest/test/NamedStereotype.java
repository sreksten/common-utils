package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.inheritance.stereotypeinheritencetest.test;

import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Stereotype
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Named
@Inherited
public @interface NamedStereotype {
}
