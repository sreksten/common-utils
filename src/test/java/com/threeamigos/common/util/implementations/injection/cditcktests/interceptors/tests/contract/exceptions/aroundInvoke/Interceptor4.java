package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.exceptions.aroundInvoke;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(400)
@ExceptionBinding
public class Interceptor4 {
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        ExceptionBean.failFirstTwoInvocations();
        return !(Boolean) ctx.proceed();
    }
}
