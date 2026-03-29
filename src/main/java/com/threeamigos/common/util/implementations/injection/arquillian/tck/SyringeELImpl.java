package com.threeamigos.common.util.implementations.injection.arquillian.tck;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.cdi.tck.spi.EL;

/**
 * Basic EL SPI implementation backed by BeanManager ELResolver.
 */
public class SyringeELImpl implements EL {

    @Override
    public <T> T evaluateValueExpression(BeanManager beanManager, String expression, Class<T> expectedType) {
        ELContext context = createELContext(beanManager);
        ValueExpression valueExpression = ExpressionFactory.newInstance()
                .createValueExpression(context, expression, expectedType);
        return expectedType.cast(valueExpression.getValue(context));
    }

    @Override
    public <T> T evaluateMethodExpression(BeanManager beanManager,
                                          String expression,
                                          Class<T> expectedType,
                                          Class<?>[] expectedParamTypes,
                                          Object[] expectedParams) {
        ELContext context = createELContext(beanManager);
        MethodExpression methodExpression = ExpressionFactory.newInstance()
                .createMethodExpression(context, expression, expectedType, expectedParamTypes);
        Object result = methodExpression.invoke(context, expectedParams);
        return expectedType.cast(result);
    }

    @Override
    public ELContext createELContext(final BeanManager beanManager) {
        ExpressionFactory expressionFactory = beanManager.wrapExpressionFactory(ExpressionFactory.newInstance());
        StandardELContext context = new StandardELContext(expressionFactory);
        ELResolver resolver = beanManager.getELResolver();
        context.addELResolver(resolver);
        return new DelegatingELContext(context, resolver);
    }

    private static final class DelegatingELContext extends ELContext {

        private final ELContext delegate;
        private final ELResolver resolver;

        private DelegatingELContext(ELContext delegate, ELResolver resolver) {
            this.delegate = delegate;
            this.resolver = resolver;
        }

        @Override
        public ELResolver getELResolver() {
            return resolver;
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return delegate.getFunctionMapper();
        }

        @Override
        public VariableMapper getVariableMapper() {
            return delegate.getVariableMapper();
        }
    }
}
