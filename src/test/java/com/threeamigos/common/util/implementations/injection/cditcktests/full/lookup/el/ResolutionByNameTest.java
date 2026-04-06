package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.el;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolutionByNameTest {

    @Test
    void testQualifiedNameLookup() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            ELContext context = createELContext(beanManager);
            String value1 = resolvePath(beanManager, context, "game.value", String.class);
            String value2 = resolvePath(beanManager, context, "game.value", String.class);
            Boolean result = ("foo".equals(value1) && "foo".equals(value2)) ? "foo".equals(value1) : Boolean.FALSE;
            assertTrue(result);
            assertEquals(1, getContextualReference(beanManager, Counter.class).getCount());
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testContextCreatesNewInstanceForInjection() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            Context requestContext = beanManager.getContext(RequestScoped.class);
            Bean<Tuna> tunaBean = getBean(beanManager, Tuna.class);
            assertNull(requestContext.get(tunaBean));

            TunaFarm tunaFarm = resolvePath(beanManager, createELContext(beanManager), "tunaFarm", TunaFarm.class);
            assertNotNull(tunaFarm.tuna);
            long timestamp = tunaFarm.tuna.getTimestamp();

            Tuna tuna = requestContext.get(tunaBean);
            assertNotNull(tuna);
            assertEquals(timestamp, tuna.getTimestamp());
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testUnresolvedNameReturnsNull() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            ELContext context = createELContext(beanManager);
            Object value = beanManager.getELResolver().getValue(context, null, "nonExistingTuna");
            assertNull(value);
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testELResolverReturnsContextualInstance() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            Salmon salmon = getContextualReference(beanManager, Salmon.class);
            salmon.setAge(3);

            Integer age = resolvePath(beanManager, createELContext(beanManager), "salmon.age", Integer.class);
            assertEquals(Integer.valueOf(3), age);
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testBeanNameWithSeparatedListOfELIdentifiers() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = null;
        try {
            syringe.setup();
            beanManager = (BeanManagerImpl) syringe.getBeanManager();
            beanManager.getContextManager().activateRequest();

            GoldenFish goldenFish = resolvePath(beanManager, createELContext(beanManager), "magic.golden.fish", GoldenFish.class);
            assertNotNull(goldenFish);
        } finally {
            if (beanManager != null) {
                beanManager.getContextManager().deactivateRequest();
            }
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Counter.class,
                Game.class,
                GoldenFish.class,
                Salmon.class,
                Tuna.class,
                TunaFarm.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> T resolvePath(BeanManager beanManager, ELContext context, String expression, Class<T> expectedType) {
        String path = normalizeExpression(expression);
        Object base = null;
        for (String segment : path.split("\\.")) {
            if (segment.isEmpty()) {
                continue;
            }
            context.setPropertyResolved(false);
            base = beanManager.getELResolver().getValue(context, base, segment);
            if (!context.isPropertyResolved()) {
                return null;
            }
        }
        return expectedType.cast(base);
    }

    private String normalizeExpression(String expression) {
        if (expression == null) {
            return "";
        }
        String trimmed = expression.trim();
        if (trimmed.startsWith("#{") && trimmed.endsWith("}") && trimmed.length() > 3) {
            return trimmed.substring(2, trimmed.length() - 1);
        }
        return trimmed;
    }

    private ELContext createELContext(BeanManager beanManager) {
        return new SimpleELContext(beanManager.getELResolver());
    }

    private <T> Bean<T> getBean(BeanManager beanManager, Class<T> type) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Set<Bean<?>> beans = (Set) beanManager.getBeans(type);
        @SuppressWarnings("unchecked")
        Bean<T> bean = (Bean<T>) beanManager.resolve(beans);
        return bean;
    }

    private <T> T getContextualReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = getBean(beanManager, type);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        return type.cast(beanManager.getReference(bean, type, creationalContext));
    }

    private static final class SimpleELContext extends ELContext {
        private final ELResolver resolver;

        private SimpleELContext(ELResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public ELResolver getELResolver() {
            return resolver;
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return null;
        }

        @Override
        public VariableMapper getVariableMapper() {
            return null;
        }
    }
}
