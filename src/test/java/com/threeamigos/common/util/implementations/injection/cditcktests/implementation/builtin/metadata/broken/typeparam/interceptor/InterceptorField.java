package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.interceptor;

import com.threeamigos.common.util.implementations.injection.cditcktests.implementation.builtin.metadata.broken.typeparam.Cream;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

@Binding
@jakarta.interceptor.Interceptor
@Priority(100)
public class InterceptorField {

    @SuppressWarnings("unused")
    @Inject
    private Interceptor<Cream> interceptor;

    @AroundInvoke
    public Object alwaysReturnThis(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
