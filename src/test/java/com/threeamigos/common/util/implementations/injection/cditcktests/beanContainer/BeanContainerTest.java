package com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.AnimalStereotype;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.ContextRegisteringExtension;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.CustomScoped;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.Dog;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.Food;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.NoImplScope;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.Soy;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.Tame;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.Terrier;
import com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures.Transactional;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanContainerTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.beanContainer.beancontainertest.fixtures";

    @Test
    void testAmbiguousDependencyResolved() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            Set<Bean<?>> beans = beanContainer.getBeans(Food.class);
            assertEquals(2, beans.size());
            Bean<?> bean = beanContainer.resolve(beans);
            assertNotNull(bean);
            assertTrue(bean.isAlternative());
            assertTrue(typeSetMatches(bean.getTypes(), Food.class, Soy.class, Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAmbiguousDependencyNotResolved() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            Set<Bean<?>> beans = new HashSet<Bean<?>>();
            beans.addAll(beanContainer.getBeans(Dog.class));
            beans.addAll(beanContainer.getBeans(Terrier.class));
            assertThrows(AmbiguousResolutionException.class, () -> beanContainer.resolve(beans));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDetermineQualifierType() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertTrue(beanContainer.isQualifier(Any.class));
            assertTrue(beanContainer.isQualifier(Tame.class));
            assertFalse(beanContainer.isQualifier(AnimalStereotype.class));
            assertFalse(beanContainer.isQualifier(ApplicationScoped.class));
            assertFalse(beanContainer.isQualifier(Transactional.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDetermineScope() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertTrue(beanContainer.isScope(ApplicationScoped.class));
            assertFalse(beanContainer.isScope(Tame.class));
            assertFalse(beanContainer.isScope(AnimalStereotype.class));
            assertFalse(beanContainer.isScope(Transactional.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDetermineStereotype() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertTrue(beanContainer.isStereotype(AnimalStereotype.class));
            assertFalse(beanContainer.isStereotype(Tame.class));
            assertFalse(beanContainer.isStereotype(ApplicationScoped.class));
            assertFalse(beanContainer.isStereotype(Transactional.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDetermineInterceptorBindingType() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertTrue(beanContainer.isInterceptorBinding(Transactional.class));
            assertFalse(beanContainer.isInterceptorBinding(Tame.class));
            assertFalse(beanContainer.isInterceptorBinding(AnimalStereotype.class));
            assertFalse(beanContainer.isInterceptorBinding(ApplicationScoped.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDetermineScopeType() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertTrue(beanContainer.isNormalScope(RequestScoped.class));
            assertTrue(beanContainer.isNormalScope(SessionScoped.class));
            assertFalse(beanContainer.isNormalScope(Dependent.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveWithNull() {
        Syringe syringe = newSyringe();
        try {
            assertNull(syringe.getBeanManager().resolve(null));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveWithEmptySet() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();
            assertNull(beanContainer.resolve(Collections.<Bean<? extends Integer>>emptySet()));
            assertNull(beanContainer.resolve(new HashSet<Bean<? extends String>>()));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetContexts() {
        Syringe syringe = newSyringe();
        try {
            BeanContainer beanContainer = syringe.getBeanManager();

            Collection<Context> noImpl = beanContainer.getContexts(NoImplScope.class);
            assertEquals(0, noImpl.size());

            Collection<Context> customContextImpls = beanContainer.getContexts(CustomScoped.class);
            assertEquals(2, customContextImpls.size());

            Collection<Context> builtInContextImpls = beanContainer.getContexts(Singleton.class);
            assertTrue(builtInContextImpls.size() >= 1);
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addBuildCompatibleExtension(ContextRegisteringExtension.class.getName());
        syringe.setup();
        return syringe;
    }

    private static boolean typeSetMatches(Set<Type> actualTypes, Type... expectedTypes) {
        Set<Type> expected = new HashSet<Type>();
        Collections.addAll(expected, expectedTypes);
        return actualTypes.equals(expected);
    }
}
