package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.interceptor;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Stereotype;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StereotypeWithMultipleInterceptorBindingsTest {

    @Test
    void testMultipleInterceptorBindings() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(),
                Foo.class,
                Alpha.class,
                Omega.class,
                AlphaOmegaStereotype.class,
                AlphaInterceptor.class,
                OmegaInterceptor.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            Foo foo = syringe.getBeanManager().createInstance().select(Foo.class).get();
            assertNotNull(foo);
            foo.getInspections().clear();

            foo.ping();

            assertTrue(foo.getInspections().contains(AlphaInterceptor.class.getName()));
            assertTrue(foo.getInspections().contains(OmegaInterceptor.class.getName()));
        } finally {
            syringe.shutdown();
        }
    }

    @Target({TYPE, METHOD})
    @Retention(RUNTIME)
    @Documented
    @Inherited
    @InterceptorBinding
    public @interface Alpha {
        boolean value();
    }

    @Target({TYPE, METHOD})
    @Retention(RUNTIME)
    @Documented
    @Inherited
    @InterceptorBinding
    public @interface Omega {
    }

    @Alpha(value = false)
    @Omega
    @Stereotype
    @Target({TYPE, METHOD, FIELD})
    @Retention(RUNTIME)
    public @interface AlphaOmegaStereotype {
    }

    @Dependent
    @Alpha(value = true)
    @AlphaOmegaStereotype
    public static class Foo {
        private final List<String> inspections = new ArrayList<String>();

        public void ping() {
        }

        public List<String> getInspections() {
            return inspections;
        }
    }

    @Alpha(value = true)
    @Interceptor
    @Priority(Interceptor.Priority.APPLICATION)
    public static class AlphaInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            Object target = context.getTarget();
            if (target instanceof Foo) {
                ((Foo) target).getInspections().add(getClass().getName());
            }
            return context.proceed();
        }
    }

    @Omega
    @Interceptor
    @Priority(Interceptor.Priority.APPLICATION + 1)
    public static class OmegaInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            Object target = context.getTarget();
            if (target instanceof Foo) {
                ((Foo) target).getInspections().add(getClass().getName());
            }
            return context.proceed();
        }
    }
}
