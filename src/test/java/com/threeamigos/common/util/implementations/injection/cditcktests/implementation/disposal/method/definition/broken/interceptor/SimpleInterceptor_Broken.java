package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.broken.interceptor;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Disposes;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Secure
@Priority(2)
public class SimpleInterceptor_Broken {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }

    public void dispose(@Disposes @ProducedString String foo) {
    }
}
