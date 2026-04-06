package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.exceptions.aroundInvoke;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(300)
@ExceptionBinding
public class Interceptor3 {
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        try {
            ExceptionBean.failFirstTwoInvocations();
        } catch (RuntimeException e) {
            // OK - first invocation
        }
        try {
            ctx.proceed();
        } catch (RuntimeException e) {
            // OK - second invocation
        }
        return ctx.proceed();
    }
}
