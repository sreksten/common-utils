package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Factory for creating Producer instances for fields and methods.
 *
 * <p>This factory creates Producer objects that handle producer method/field invocation
 * and disposer method invocation.
 *
 * <p><b>Usage in Portable Extensions:</b>
 * <pre>{@code
 * public class MyExtension implements Extension {
 *     void processProducer(@Observes ProcessProducer<MyBean, MyProduct> event, BeanManager bm) {
 *         AnnotatedMethod<MyBean> method = event.getAnnotatedProducerMethod();
 *         Bean<MyBean> declaringBean = event.getBean();
 *
 *         ProducerFactory<MyProduct> factory = bm.getProducerFactory(method, declaringBean);
 *         Producer<MyProduct> producer = factory.createProducer(declaringBean);
 *     }
 * }
 * }</pre>
 *
 * @param <X> the produced type
 * @author Stefano Reksten
 */
public class ProducerFactoryImpl<X> implements ProducerFactory<X> {

    private final AnnotatedMember<?> producerMember;
    private final BeanManager beanManager;

    /**
     * Creates a producer factory for a field.
     *
     * @param field the producer field
     * @param beanManager the bean manager
     */
    public ProducerFactoryImpl(AnnotatedField<?> field, BeanManager beanManager) {
        this.producerMember = field;
        this.beanManager = beanManager;
    }

    /**
     * Creates a producer factory for a method.
     *
     * @param method the producer method
     * @param beanManager the bean manager
     */
    public ProducerFactoryImpl(AnnotatedMethod<?> method, BeanManager beanManager) {
        this.producerMember = method;
        this.beanManager = beanManager;
    }

    /**
     * Creates a Producer for the configured producer field/method.
     *
     * <p>The Producer handles:
     * <ul>
     *   <li>Producing instances by invoking the producer method/field</li>
     *   <li>Disposing instances by invoking the disposer method (if present)</li>
     *   <li>Tracking injection points for parameters</li>
     * </ul>
     *
     * @param bean the declaring bean
     * @param <T> the bean type
     * @return the producer
     */
    @Override
    public <T> Producer<T> createProducer(Bean<T> bean) {
        if (producerMember instanceof AnnotatedField) {
            return new FieldProducerImpl<>((AnnotatedField<?>) producerMember, bean, beanManager);
        } else if (producerMember instanceof AnnotatedMethod) {
            return new MethodProducerImpl<>((AnnotatedMethod<?>) producerMember, bean, beanManager);
        } else {
            throw new IllegalStateException("Producer member must be field or method");
        }
    }

    /**
     * Producer implementation for producer fields.
     */
    private static class FieldProducerImpl<T> implements Producer<T> {
        private final AnnotatedField<?> field;
        private final Bean<?> declaringBean;
        private final BeanManager beanManager;

        public FieldProducerImpl(AnnotatedField<?> field, Bean<?> declaringBean, BeanManager beanManager) {
            this.field = field;
            this.declaringBean = declaringBean;
            this.beanManager = beanManager;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T produce(CreationalContext<T> ctx) {
            try {
                // Get declaring bean instance
                Object declaringInstance = beanManager.getReference(
                    declaringBean,
                    declaringBean.getBeanClass(),
                    ctx
                );

                // Read field value
                Field javaField = field.getJavaMember();
                javaField.setAccessible(true);
                return (T) javaField.get(declaringInstance);

            } catch (Exception e) {
                throw new RuntimeException("Failed to produce from field " + field.getJavaMember().getName(), e);
            }
        }

        @Override
        public void dispose(T instance) {
            // Producer fields don't have disposer methods
            // Disposal is handled by the scope
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            // Producer fields have no injection points
            return Collections.emptySet();
        }
    }

    /**
     * Producer implementation for producer methods.
     */
    private static class MethodProducerImpl<T> implements Producer<T> {
        private final AnnotatedMethod<?> method;
        private final Bean<?> declaringBean;
        private final BeanManager beanManager;
        private final Set<InjectionPoint> injectionPoints;

        public MethodProducerImpl(AnnotatedMethod<?> method, Bean<?> declaringBean, BeanManager beanManager) {
            this.method = method;
            this.declaringBean = declaringBean;
            this.beanManager = beanManager;
            this.injectionPoints = discoverInjectionPoints();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T produce(CreationalContext<T> ctx) {
            try {
                // Get declaring bean instance
                Object declaringInstance = beanManager.getReference(
                    declaringBean,
                    declaringBean.getBeanClass(),
                    ctx
                );

                // Resolve method parameters
                Method javaMethod = method.getJavaMember();
                javaMethod.setAccessible(true);
                Object[] args = resolveMethodParameters(javaMethod, ctx);

                // Invoke producer method
                return (T) javaMethod.invoke(declaringInstance, args);

            } catch (Exception e) {
                throw new RuntimeException("Failed to produce from method " + method.getJavaMember().getName(), e);
            }
        }

        @Override
        public void dispose(T instance) {
            // Disposer methods are tracked separately
            // For now, this is a no-op
            // Full implementation would find and invoke the disposer method
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return injectionPoints;
        }

        private Set<InjectionPoint> discoverInjectionPoints() {
            Set<InjectionPoint> points = new HashSet<>();
            Method javaMethod = method.getJavaMember();

            for (java.lang.reflect.Parameter param : javaMethod.getParameters()) {
                points.add(new InjectionPointImpl(param, declaringBean));
            }

            return points;
        }

        private Object[] resolveMethodParameters(Method javaMethod, CreationalContext<T> ctx) {
            java.lang.reflect.Parameter[] params = javaMethod.getParameters();
            Object[] args = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                InjectionPoint ip = new InjectionPointImpl(params[i], declaringBean);
                args[i] = beanManager.getInjectableReference(ip, ctx);
            }

            return args;
        }
    }
}
