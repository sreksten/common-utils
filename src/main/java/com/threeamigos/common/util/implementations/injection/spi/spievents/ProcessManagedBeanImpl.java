package com.threeamigos.common.util.implementations.injection.spi.spievents;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.invoke.InvokerBuilder;
import jakarta.enterprise.inject.spi.ProcessManagedBean;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * ProcessManagedBean event implementation.
 *
 * <p>Fired for each discovered managed bean (non-producer). Extensions can
 * inspect or veto via {@link #addDefinitionError(Throwable)}.</p>
 *
 * @param <X> bean type
 */
public class ProcessManagedBeanImpl<X> extends ProcessBeanImpl<X> implements ProcessManagedBean<X> {

    private final AnnotatedType<X> annotatedType;

    public ProcessManagedBeanImpl(Bean<X> bean, AnnotatedType<X> annotatedType, BeanManager beanManager) {
        super(bean, annotatedType, beanManager);
        this.annotatedType = annotatedType;
    }

    @Override
    public AnnotatedType<X> getAnnotatedBeanClass() {
        return annotatedType;
    }

    @Override
    public InvokerBuilder<Invoker<X, ?>> createInvoker(AnnotatedMethod<? super X> method) {
        if (method == null) {
            throw new IllegalArgumentException("AnnotatedMethod cannot be null");
        }
        Method javaMethod = method.getJavaMember();
        return new SimpleInvokerBuilder<>(javaMethod);
    }

    /**
     * Minimal InvokerBuilder that reflects directly on the underlying Java Method.
     * withInstanceLookup/withArgumentLookup are no-ops for this simple implementation.
     */
    private static class SimpleInvokerBuilder<X> implements InvokerBuilder<Invoker<X, ?>> {
        private final Method javaMethod;

        SimpleInvokerBuilder(Method javaMethod) {
            this.javaMethod = javaMethod;
        }

        @Override
        public InvokerBuilder<Invoker<X, ?>> withInstanceLookup() {
            return this;
        }

        @Override
        public InvokerBuilder<Invoker<X, ?>> withArgumentLookup(int position) {
            return this;
        }

        @Override
        public Invoker<X, ?> build() {
            return (instance, parameters) -> {
                try {
                    if (!javaMethod.isAccessible()) {
                        javaMethod.setAccessible(true);
                    }
                    return javaMethod.invoke(instance, parameters);
                } catch (InvocationTargetException e) {
                    Throwable target = e.getTargetException();
                    if (target instanceof Exception) {
                        throw (Exception) target;
                    }
                    throw new RuntimeException(target);
                } catch (Exception e) {
                    throw e;
                }
            };
        }
    }
}
