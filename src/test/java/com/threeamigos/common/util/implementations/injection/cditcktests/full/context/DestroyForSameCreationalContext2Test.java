package com.threeamigos.common.util.implementations.injection.cditcktests.full.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;

class DestroyForSameCreationalContext2Test {

    @Test
    void testDestroyForSameCreationalContextOnly() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), BootstrapAnchor.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            Context sessionContext = beanManager.getContext(SessionScoped.class);
            String sessionId = "full-context-destroy-same-cc-2";

            InspectableContextual contextual = new InspectableContextual();
            CreationalContext<String> creationalContext = beanManager.createCreationalContext(contextual);

            beanManager.getContextManager().activateSession(sessionId);
            try {
                sessionContext.get(contextual, creationalContext);
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }

            assertSame(contextual.getCreationalContextPassedToCreate(), contextual.getCreationalContextPassedToDestroy());
        } finally {
            syringe.shutdown();
        }
    }

    private static class InspectableContextual implements Bean<String> {

        private CreationalContext<String> creationalContextPassedToCreate;
        private CreationalContext<String> creationalContextPassedToDestroy;

        @Override
        public String create(CreationalContext<String> creationalContext) {
            creationalContextPassedToCreate = creationalContext;
            return "123";
        }

        @Override
        public void destroy(String instance, CreationalContext<String> creationalContext) {
            creationalContextPassedToDestroy = creationalContext;
        }

        @Override
        public Class<?> getBeanClass() {
            return String.class;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            Set<Annotation> qualifiers = new HashSet<Annotation>();
            qualifiers.add(Default.Literal.INSTANCE);
            qualifiers.add(Any.Literal.INSTANCE);
            return qualifiers;
        }

        @Override
        public Class<? extends Annotation> getScope() {
            return SessionScoped.class;
        }

        @Override
        public Set<Class<? extends Annotation>> getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public Set<Type> getTypes() {
            Set<Type> types = new HashSet<Type>();
            types.add(String.class);
            types.add(Object.class);
            return types;
        }

        @Override
        public boolean isAlternative() {
            return false;
        }

        CreationalContext<String> getCreationalContextPassedToCreate() {
            return creationalContextPassedToCreate;
        }

        CreationalContext<String> getCreationalContextPassedToDestroy() {
            return creationalContextPassedToDestroy;
        }
    }

    @SessionScoped
    public static class BootstrapAnchor implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
