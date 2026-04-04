package com.threeamigos.common.util.implementations.injection.cditcktests.context;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;

class DestroyForSameCreationalContext2Test {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.destroyforsamecreationalcontext2test.testdestroyforsamecreationalcontextonly";

    @Test
    void testDestroyForSameCreationalContextOnly() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                Context requestContext = syringe.getBeanManager().getContext(RequestScoped.class);
                InspectableContextual contextual = new InspectableContextual();
                CreationalContext<String> creationalContext = syringe.getBeanManager().createCreationalContext(contextual);

                requestContext.get(contextual, creationalContext);
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }

            InspectableContextual contextual = InspectableContextual.lastCreated();
            assertSame(contextual.getCreationalContextPassedToCreate(), contextual.getCreationalContextPassedToDestroy());
        } finally {
            syringe.shutdown();
        }
    }

    private static class InspectableContextual implements Bean<String> {

        private static InspectableContextual lastCreated;

        private CreationalContext<String> creationalContextPassedToCreate;
        private CreationalContext<String> creationalContextPassedToDestroy;

        private InspectableContextual() {
            lastCreated = this;
        }

        static InspectableContextual lastCreated() {
            return lastCreated;
        }

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
            return RequestScoped.class;
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

        public CreationalContext<String> getCreationalContextPassedToCreate() {
            return creationalContextPassedToCreate;
        }

        public CreationalContext<String> getCreationalContextPassedToDestroy() {
            return creationalContextPassedToDestroy;
        }
    }
}
