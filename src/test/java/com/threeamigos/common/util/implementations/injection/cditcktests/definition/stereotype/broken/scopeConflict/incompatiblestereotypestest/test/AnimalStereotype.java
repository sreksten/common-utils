package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.scopeConflict.incompatiblestereotypestest.test;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Stereotype
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@RequestScoped
public @interface AnimalStereotype {
}
