package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.fires;

import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;
import jakarta.enterprise.inject.spi.ProcessObserverMethod;
import jakarta.enterprise.inject.spi.ProcessProducer;
import jakarta.enterprise.inject.spi.ProcessProducerField;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;
import jakarta.enterprise.inject.spi.ProcessSessionBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class ContainerLifecycleEvents {

    public static final BeforeBeanDiscovery BEFORE_BEAN_DISCOVERY = proxy(BeforeBeanDiscovery.class);
    public static final AfterBeanDiscovery AFTER_BEAN_DISCOVERY = proxy(AfterBeanDiscovery.class);
    public static final AfterDeploymentValidation AFTER_DEPLOYMENT_VALIDATION = proxy(AfterDeploymentValidation.class);
    public static final BeforeShutdown BEFORE_SHUTDOWN = proxy(BeforeShutdown.class);
    public static final ProcessAnnotatedType<Integer> PROCESS_ANNOTATED_TYPE = proxy(ProcessAnnotatedType.class);
    public static final ProcessInjectionPoint<String, Number> PROCESS_INJECTION_POINT = proxy(ProcessInjectionPoint.class);
    public static final ProcessInjectionTarget<Double> PROCESS_INJECTION_TARGET = proxy(ProcessInjectionTarget.class);
    public static final ProcessProducer<Number, String> PROCESS_PRODUCER = proxy(ProcessProducer.class);
    public static final ProcessBeanAttributes<Long> PROCESS_BEAN_ATTRIBUTES = proxy(ProcessBeanAttributes.class);
    public static final ProcessBean<Character> PROCESS_BEAN = proxy(ProcessBean.class);
    public static final ProcessObserverMethod<Short, Byte> PROCESS_OBSERVER_METHOD = proxy(ProcessObserverMethod.class);
    public static final ProcessSessionBean<Float> PROCESS_SESSION_BEAN = proxy(ProcessSessionBean.class);
    public static final ProcessProducerField<Integer, Long> PROCESS_PRODUCER_FIELD = proxy(ProcessProducerField.class);
    public static final ProcessProducerMethod<Integer, Long> PROCESS_PRODUCER_METHOD = proxy(ProcessProducerMethod.class);

    private ContainerLifecycleEvents() {
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<?> eventType) {
        ClassLoader loader = eventType.getClassLoader();
        Class<?>[] interfaces = new Class<?>[]{eventType};
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return defaultValue(method.getReturnType());
            }
        };
        return (T) Proxy.newProxyInstance(loader, interfaces, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (Boolean.TYPE.equals(returnType)) {
            return Boolean.FALSE;
        }
        if (Byte.TYPE.equals(returnType)) {
            return Byte.valueOf((byte) 0);
        }
        if (Short.TYPE.equals(returnType)) {
            return Short.valueOf((short) 0);
        }
        if (Integer.TYPE.equals(returnType)) {
            return Integer.valueOf(0);
        }
        if (Long.TYPE.equals(returnType)) {
            return Long.valueOf(0L);
        }
        if (Float.TYPE.equals(returnType)) {
            return Float.valueOf(0F);
        }
        if (Double.TYPE.equals(returnType)) {
            return Double.valueOf(0D);
        }
        if (Character.TYPE.equals(returnType)) {
            return Character.valueOf((char) 0);
        }
        return null;
    }
}
