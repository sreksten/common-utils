package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
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
 * @param <T> bean type
 */
public class ProcessManagedBeanImpl<T> extends ProcessBeanImpl<T> implements ProcessManagedBean<T> {

    private final AnnotatedType<T> annotatedType;

    public ProcessManagedBeanImpl(MessageHandler messageHandler, KnowledgeBase knowledgeBase, Bean<T> bean,
                                  AnnotatedType<T> annotatedType) {
        super(messageHandler, knowledgeBase, bean, annotatedType);
        this.annotatedType = annotatedType;
    }

    @Override
    public AnnotatedType<T> getAnnotatedBeanClass() {
        return annotatedType;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        knowledgeBase.addDefinitionError(Phase.PROCESS_MANAGED_BEAN, "Definition error for " +
                bean.getBeanClass().getName(), t);
    }
    @Override
    public InvokerBuilder<Invoker<T, ?>> createInvoker(AnnotatedMethod<? super T> method) {
        checkNotNull(method, "AnnotatedMethod");
        Method javaMethod = method.getJavaMember();
        return new SimpleInvokerBuilder<>(javaMethod);
    }

    /**
     * Minimal InvokerBuilder that reflects directly on the underlying Java Method.
     * withInstanceLookup/withArgumentLookup are no-ops for this simple implementation.
     */
    private static class SimpleInvokerBuilder<T> implements InvokerBuilder<Invoker<T, ?>> {
        private final Method javaMethod;

        SimpleInvokerBuilder(Method javaMethod) {
            this.javaMethod = javaMethod;
        }

        @Override
        public InvokerBuilder<Invoker<T, ?>> withInstanceLookup() {
            return this;
        }

        @Override
        public InvokerBuilder<Invoker<T, ?>> withArgumentLookup(int position) {
            return this;
        }

        @Override
        public Invoker<T, ?> build() {
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
                }
            };
        }
    }
}
