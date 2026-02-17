package com.threeamigos.common.util.implementations.injection.scopes;

import jakarta.inject.Scope;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomScope {
}
