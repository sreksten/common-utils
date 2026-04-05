package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.el;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WrapExpressionFactoryTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                DummyExpressionFactory.class,
                DummyMethodExpression.class,
                DummyValueExpression.class,
                Foo.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testWrapping() {
//        ActionSequence.reset();
//
//        ExpressionFactory wrappedExpressionFactory = beanManager.wrapExpressionFactory(new DummyExpressionFactory());
//
//        MethodExpression methodExpression = wrappedExpressionFactory.createMethodExpression(null, "foo.test", String.class, null);
//        Object methodElResult = methodExpression.invoke(createELContext(beanManager, wrappedExpressionFactory), null);
//        assertNotNull(methodElResult);
//        assertTrue(methodElResult instanceof Integer);
//        assertEquals(Integer.valueOf(-1), methodElResult);
//
//        List<String> fooSingleton = Collections.singletonList(Foo.class.getName());
//        assertEquals(fooSingleton, ActionSequence.getSequenceData("create"));
//        assertEquals(fooSingleton, ActionSequence.getSequenceData("destroy"));
//        ActionSequence.reset();
//
//        ValueExpression valueExpression = wrappedExpressionFactory.createValueExpression(null, "foo.test", String.class);
//        Object valElResult = valueExpression.getValue(createELContext(beanManager, wrappedExpressionFactory));
//        assertNotNull(valElResult);
//        assertTrue(valElResult instanceof Integer);
//        assertEquals(Integer.valueOf(-1), valElResult);
//
//        assertEquals(fooSingleton, ActionSequence.getSequenceData("create"));
//        assertEquals(fooSingleton, ActionSequence.getSequenceData("destroy"));
    }

    private static ELContext createELContext(final BeanManager beanManager,
                                             final ExpressionFactory expressionFactory) {
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
