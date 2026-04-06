package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.invocationContext;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InvocationContextTest {

    @Test
    void testGetTargetMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            SimpleBean instance = getContextualReference(syringe, SimpleBean.class);
            instance.setId(10);
            assertEquals(10, instance.getId());
            assertEquals(10, Interceptor1.getTarget().getId());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetTimerMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertTrue(getContextualReference(syringe, SimpleBean.class).testGetTimer());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetMethodForAroundInvokeInterceptorMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertTrue(getContextualReference(syringe, SimpleBean.class).testGetMethod());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetMethodForLifecycleCallbackInterceptorMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, SimpleBean.class);
            assertTrue(PostConstructInterceptor.isGetMethodReturnsNull());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testCtxProceedForLifecycleCallbackInterceptorMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, SimpleBean.class);
            assertTrue(PostConstructInterceptor.isCtxProceedReturnsNull());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMethodParameters() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals(5, getContextualReference(syringe, SimpleBean.class).add(1, 2));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIllegalNumberOfParameters() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertThrows(IllegalArgumentException.class, () ->
                    getContextualReference(syringe, SimpleBean.class).add2(1, 1));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testIllegalTypeOfParameters() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertThrows(IllegalArgumentException.class, () ->
                    getContextualReference(syringe, SimpleBean.class).add3(1, 1));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProceedReturnsNullForVoidMethod() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, SimpleBean.class).voidMethod();
            assertTrue(Interceptor7.isProceedReturnsNull());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testContextData() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            getContextualReference(syringe, SimpleBean.class).foo();
            assertTrue(Interceptor8.isContextDataOK());
            assertTrue(Interceptor9.isContextDataOK());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBusinessMethodNotCalledWithoutProceedInvocation() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertEquals("foo", getContextualReference(syringe, SimpleBean.class).echo("foo"));
            assertFalse(SimpleBean.isEchoCalled());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetInterceptorBindings() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            assertTrue(getContextualReference(syringe, SimpleBean.class).bindings());
            assertEquals(setOf(
                            new SimplePCBinding.Literal(),
                            new PseudoBinding.Literal(),
                            new AroundConstructBinding1.Literal(),
                            new AroundConstructBinding2.Literal(),
                            new Binding16.Literal("class-level"),
                            new SuperBinding.Literal()
                    ),
                    AroundConstructInterceptor1.getAllBindings());
            assertEquals(AroundConstructInterceptor1.getAllBindings(), AroundConstructInterceptor2.getAllBindings());
            assertEquals(setOf(
                            new SimplePCBinding.Literal(),
                            new PseudoBinding.Literal(),
                            new AroundConstructBinding1.Literal(),
                            new Binding16.Literal("class-level"),
                            new SuperBinding.Literal()
                    ),
                    PostConstructInterceptor.getAllBindings());
            assertEquals(setOf(
                            new SimplePCBinding.Literal(),
                            new PseudoBinding.Literal(),
                            new AroundConstructBinding1.Literal(),
                            new Binding11.Literal(),
                            new Binding12.Literal(),
                            new Binding13.Literal("ko"),
                            new Binding14.Literal("foobar"),
                            new Binding15.Literal(),
                            new Binding15Additional.Literal("AdditionalBinding"),
                            new Binding16.Literal("method-level"),
                            new SuperBinding.Literal()
                    ),
                    Interceptor12.getAllBindings());
            assertEquals(setOf(new Binding12.Literal()), Interceptor12.getBinding12s());
            assertEquals(new Binding12.Literal(), Interceptor12.getBinding12());
            assertEquals(setOf(), Interceptor12.getBinding5s());
            assertNull(Interceptor12.getBinding6());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AroundConstructBinding1.class,
                AroundConstructBinding2.class,
                AroundConstructInterceptor1.class,
                AroundConstructInterceptor2.class,
                Binding1.class,
                Binding2.class,
                Binding3.class,
                Binding4.class,
                Binding5.class,
                Binding6.class,
                Binding7.class,
                Binding10.class,
                Binding11.class,
                Binding12.class,
                Binding13.class,
                Binding14.class,
                Binding15.class,
                Binding15Additional.class,
                Binding16.class,
                Interceptor1.class,
                Interceptor2.class,
                Interceptor3.class,
                Interceptor4.class,
                Interceptor5.class,
                Interceptor6.class,
                Interceptor7.class,
                Interceptor8.class,
                Interceptor9.class,
                Interceptor10.class,
                Interceptor11.class,
                Interceptor12.class,
                Interceptor13.class,
                Interceptor14.class,
                PostConstructInterceptor.class,
                PseudoBinding.class,
                SimpleBean.class,
                SimpleBinding.class,
                SimplePCBinding.class,
                SuperBinding.class,
                SuperClass.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> beanClass) {
        return syringe.getBeanManager().createInstance().select(beanClass).get();
    }

    private static <T> Set<T> setOf(T... values) {
        return new HashSet<T>(Arrays.asList(values));
    }
}
