package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.scopeConflict.incompatiblestereotypestest.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Stereotype
@Target(TYPE)
@Retention(RUNTIME)
@ApplicationScoped
public @interface FishStereotype {
}
