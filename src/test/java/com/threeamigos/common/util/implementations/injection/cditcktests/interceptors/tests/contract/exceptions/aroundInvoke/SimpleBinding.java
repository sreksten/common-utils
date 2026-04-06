package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.exceptions.aroundInvoke;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@InterceptorBinding
@Inherited
@Target({TYPE})
@Retention(RUNTIME)
public @interface SimpleBinding {
}
