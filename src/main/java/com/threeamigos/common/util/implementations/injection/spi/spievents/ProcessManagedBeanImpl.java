package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.util.RawTypeExtractor;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.invoke.InvokerBuilder;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.interceptor.Interceptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Set;

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
        validateTargetBean();
        validateTargetMethod(javaMethod);
        validateNonPortableTargetMethod(javaMethod);
        return new SimpleInvokerBuilder<>(javaMethod, bean.getBeanClass());
    }

    /**
     * Minimal InvokerBuilder that reflects directly on the underlying Java Method.
     * withInstanceLookup/withArgumentLookup are no-ops for this simple implementation.
     */
    private void validateTargetBean() {
        Class<?> beanClass = bean.getBeanClass();
        if (beanClass.isAnnotationPresent(jakarta.interceptor.Interceptor.class) ||
            bean instanceof Interceptor) {
            throw new DefinitionException("Cannot build invoker for interceptor bean: " + beanClass.getName());
        }
    }

    private void validateTargetMethod(Method javaMethod) {
        int modifiers = javaMethod.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            throw new DefinitionException("Cannot build invoker for private method: " + javaMethod);
        }

        if (javaMethod.getDeclaringClass().equals(Object.class) &&
            !"toString".equals(javaMethod.getName())) {
            throw new DefinitionException("Cannot build invoker for java.lang.Object method: " + javaMethod);
        }

        if (!javaMethod.getDeclaringClass().isAssignableFrom(bean.getBeanClass())) {
            throw new DefinitionException(
                "Target method is not declared on bean class or inherited from supertypes: " + javaMethod);
        }
    }

    private void validateNonPortableTargetMethod(Method javaMethod) {
        if (Modifier.isStatic(javaMethod.getModifiers())) {
            return;
        }

        Class<?> declaringClass = javaMethod.getDeclaringClass();
        if (!isDeclaringTypePresentInBeanTypes(declaringClass, bean.getTypes())) {
            throw new NonPortableBehaviourException(
                "Building invoker for non-static method declared on type not present in bean types is non-portable: " +
                    declaringClass.getName());
        }

        if (isNormalScope(bean.getScope())) {
            String reason = unproxyableReason(declaringClass);
            if (reason != null) {
                throw new NonPortableBehaviourException(
                    "Building invoker for non-static method declared on unproxyable bean type of normal-scoped bean is non-portable: " +
                        declaringClass.getName() + " (" + reason + ")");
            }
        }
    }

    private boolean isDeclaringTypePresentInBeanTypes(Class<?> declaringClass, Set<Type> beanTypes) {
        for (Type beanType : beanTypes) {
            Class<?> raw = RawTypeExtractor.getRawType(beanType);
            if (raw != null && raw.equals(declaringClass)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNormalScope(Class<? extends java.lang.annotation.Annotation> scope) {
        return scope == ApplicationScoped.class ||
            scope == RequestScoped.class ||
            scope == SessionScoped.class ||
            scope == ConversationScoped.class ||
            "javax.enterprise.context.ApplicationScoped".equals(scope.getName()) ||
            "javax.enterprise.context.RequestScoped".equals(scope.getName()) ||
            "javax.enterprise.context.SessionScoped".equals(scope.getName()) ||
            "javax.enterprise.context.ConversationScoped".equals(scope.getName());
    }

    private String unproxyableReason(Class<?> rawType) {
        if (rawType == null || rawType.equals(Object.class) || rawType.isInterface()) {
            return null;
        }
        if (rawType.isPrimitive()) {
            return "primitive type";
        }
        if (rawType.isArray()) {
            return "array type";
        }
        if (Modifier.isFinal(rawType.getModifiers())) {
            return "final class";
        }
        if (!hasNonPrivateNoArgConstructor(rawType)) {
            return "missing non-private no-arg constructor";
        }
        Method finalBusinessMethod = findNonStaticFinalNonPrivateMethod(rawType);
        if (finalBusinessMethod != null) {
            return "has non-static final method with non-private visibility: " + finalBusinessMethod.getName();
        }
        return null;
    }

    private boolean hasNonPrivateNoArgConstructor(Class<?> type) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0 && !Modifier.isPrivate(constructor.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    private Method findNonStaticFinalNonPrivateMethod(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || Modifier.isPrivate(modifiers)) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static class SimpleInvokerBuilder<T> implements InvokerBuilder<Invoker<T, ?>> {
        private final Method javaMethod;
        private final Class<?> targetBeanClass;

        SimpleInvokerBuilder(Method javaMethod, Class<?> targetBeanClass) {
            this.javaMethod = javaMethod;
            this.targetBeanClass = targetBeanClass;
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
                    if (!Modifier.isStatic(javaMethod.getModifiers())) {
                        if (instance == null) {
                            throw new IllegalArgumentException("Invoker requires non-null instance for non-static method: " + javaMethod);
                        }
                        if (!targetBeanClass.isInstance(instance)) {
                            throw new IllegalArgumentException(
                                "Invoker built for bean " + targetBeanClass.getName() +
                                " cannot be used with instance of " + instance.getClass().getName());
                        }
                    }
                    Object[] invocationArgs = adaptArguments(javaMethod, parameters);
                    if (!javaMethod.isAccessible()) {
                        javaMethod.setAccessible(true);
                    }
                    return javaMethod.invoke(instance, invocationArgs);
                } catch (InvocationTargetException e) {
                    Throwable target = e.getTargetException();
                    if (target instanceof Exception) {
                        throw (Exception) target;
                    }
                    throw new RuntimeException(target);
                }
            };
        }

        private Object[] adaptArguments(Method targetMethod, Object[] providedArguments) {
            Class<?>[] parameterTypes = targetMethod.getParameterTypes();
            int declaredParamCount = parameterTypes.length;

            if (declaredParamCount == 0) {
                return new Object[0];
            }

            if (providedArguments == null) {
                throw new RuntimeException("Arguments cannot be null for method with parameters: " + targetMethod);
            }

            if (providedArguments.length < declaredParamCount) {
                throw new RuntimeException(
                    "Not enough arguments for method " + targetMethod + ": expected " +
                        declaredParamCount + " but got " + providedArguments.length);
            }

            Object[] effectiveArgs = new Object[declaredParamCount];
            System.arraycopy(providedArguments, 0, effectiveArgs, 0, declaredParamCount);
            return effectiveArgs;
        }
    }
}
