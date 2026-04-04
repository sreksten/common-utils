package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.tooManyScopes.toomanyscopetypestest.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Stereotype
@Target(TYPE)
@Retention(RUNTIME)
@ApplicationScoped
@RequestScoped
public @interface StereotypeWithTooManyScopeTypes_Broken {
}
