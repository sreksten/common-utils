package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.dependent;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependentContextTest {

    @Test
    void testInstanceUsedForElEvaluationNotShared() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> foxBeans = beanManager.getBeans(Fox.class);
            assertTrue(foxBeans.size() == 1);

            Fox fox1 = evaluateValueExpression(beanManager, "#{fox}", Fox.class);
            Fox fox2 = evaluateValueExpression(beanManager, "#{fox}", Fox.class);
            assertNotEquals(fox1, fox2);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDependentsDestroyedWhenElEvaluationCompletes() {
        Syringe syringe = newSyringe();
        try {
            Fox.reset();
            FoxRun.setDestroyed(false);

            evaluateValueExpression(syringe.getBeanManager(), "#{foxRun}", FoxRun.class);
            assertTrue(FoxRun.isDestroyed());
            assertTrue(Fox.isDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testContextIsActiveWhenEvaluatingElExpression() {
        Syringe syringe = newSyringe();
        try {
            String foxName = evaluateMethodExpression(
                    syringe.getBeanManager(),
                    "#{sensitiveFox.getName}",
                    String.class
            );
            assertNotNull(foxName);
            assertTrue(SensitiveFox.isDependentContextActiveDuringEval());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Fox.class,
                FoxProducer.class,
                FoxRun.class,
                Pet.class,
                SensitiveFox.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    private <T> T evaluateValueExpression(BeanManager beanManager, String expression, Class<T> expectedType) {
        String beanName = parseBeanName(expression);
        @SuppressWarnings({"rawtypes", "unchecked"})
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beanManager.getBeans(beanName));
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        T instance = expectedType.cast(beanManager.getReference(bean, expectedType, creationalContext));
        destroyDependentIfNeeded(bean, instance, creationalContext);
        return instance;
    }

    private <T> T evaluateMethodExpression(BeanManager beanManager, String expression, Class<T> expectedType) {
        int start = expression.indexOf("#{");
        int dot = expression.indexOf('.', start + 2);
        int end = expression.indexOf('}', dot + 1);
        String beanName = expression.substring(start + 2, dot);
        String methodName = expression.substring(dot + 1, end);

        @SuppressWarnings({"rawtypes", "unchecked"})
        Bean<?> bean = beanManager.resolve((Set) beanManager.getBeans(beanName));
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
        Object instance = beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
        try {
            Method method = instance.getClass().getMethod(methodName);
            Object value = method.invoke(instance);
            return expectedType.cast(value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to evaluate method expression " + expression, e);
        } finally {
            destroyDependentIfNeededUnchecked(bean, instance, creationalContext);
        }
    }

    private String parseBeanName(String expression) {
        int start = expression.indexOf("#{");
        int end = expression.indexOf('}', start + 2);
        return expression.substring(start + 2, end);
    }

    private <T> void destroyDependentIfNeeded(Bean<T> bean, T instance, CreationalContext<T> creationalContext) {
        if (bean.getScope().equals(Dependent.class)) {
            bean.destroy(instance, creationalContext);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void destroyDependentIfNeededUnchecked(Bean bean, Object instance, CreationalContext creationalContext) {
        if (bean.getScope().equals(Dependent.class)) {
            bean.destroy(instance, creationalContext);
        }
    }
}
