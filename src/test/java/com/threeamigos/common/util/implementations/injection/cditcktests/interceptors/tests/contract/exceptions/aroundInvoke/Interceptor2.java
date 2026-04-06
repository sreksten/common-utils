package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.exceptions.aroundInvoke;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(200)
@SimpleBinding
public class Interceptor2 {
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed();
        } catch (RuntimeException e) {
            throw new NoSuchMethodException();
        }
    }
}
