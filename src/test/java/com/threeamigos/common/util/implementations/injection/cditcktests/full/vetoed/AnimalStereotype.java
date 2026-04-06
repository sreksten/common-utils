package com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.Vetoed;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Vetoed
@Stereotype
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@RequestScoped
public @interface AnimalStereotype {
}
