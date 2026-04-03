package com.threeamigos.common.util.implementations.injection.arquillian;

import org.jboss.arquillian.test.spi.TestEnricher;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import java.lang.reflect.Method;

/**
 * Arquillian TestEnricher for Syringe.
 * 
 * Ensures that @Inject works inside Arquillian tests when running in WildFly.
 */
public class SyringeTestEnricher implements TestEnricher {

    @Override
    public void enrich(Object testCase) {
        try {
            BeanManager beanManager = getBeanManager(testCase != null ? testCase.getClass().getClassLoader() : null);
            if (beanManager != null) {
                inject(testCase, beanManager);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not enrich test case", e);
        }
    }

    @Override
    public Object[] resolve(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] values = new Object[paramTypes.length];
        ClassLoader declaringClassLoader = method != null ? method.getDeclaringClass().getClassLoader() : null;
        BeanManager beanManager = getBeanManager(declaringClassLoader);
        if (beanManager == null) {
            return values;
        }

        for (int i = 0; i < paramTypes.length; i++) {
            try {
                values[i] = beanManager.createInstance().select(paramTypes[i]).get();
            } catch (Exception ignored) {
                values[i] = null; // Leave unresolved; Arquillian will handle nulls
            }
        }
        return values;
    }

    private void inject(Object testCase, BeanManager beanManager) {
        @SuppressWarnings("unchecked")
        Class<Object> clazz = (Class<Object>) testCase.getClass();
        AnnotatedType<Object> annotatedType = beanManager.createAnnotatedType(clazz);
        InjectionTargetFactory<Object> factory = beanManager.getInjectionTargetFactory(annotatedType);
        InjectionTarget<Object> injectionTarget = factory.createInjectionTarget(null);
        CreationalContext<Object> creationalContext = beanManager.createCreationalContext(null);
        try {
            injectionTarget.inject(testCase, creationalContext);
            injectionTarget.postConstruct(testCase);
        } catch (Exception e) {
            throw new RuntimeException("Could not enrich test case " + clazz.getName(), e);
        }
    }

    private BeanManager getBeanManager(ClassLoader preferredClassLoader) {
        BeanManager beanManager = lookupBeanManager(preferredClassLoader);
        if (beanManager != null) {
            return beanManager;
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != preferredClassLoader) {
            beanManager = lookupBeanManager(tccl);
            if (beanManager != null) {
                return beanManager;
            }
        }

        try {
            return CDI.current().getBeanManager();
        } catch (Exception e) {
            return null;
        }
    }

    private BeanManager lookupBeanManager(ClassLoader classLoader) {
        ClassLoader current = classLoader;
        while (current != null) {
            BeanManager manager = lookupSyringeRegistry(current);
            if (manager != null) {
                return manager;
            }
            current = current.getParent();
        }
        return null;
    }

    private BeanManager lookupSyringeRegistry(ClassLoader classLoader) {
        try {
            Class<?> beanManagerImplClass = Class.forName(
                    "com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl",
                    false,
                    classLoader);
            Method registryLookup = beanManagerImplClass.getMethod("getRegisteredBeanManager", ClassLoader.class);
            Object manager = registryLookup.invoke(null, classLoader);
            return manager instanceof BeanManager ? (BeanManager) manager : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
