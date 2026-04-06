package com.threeamigos.common.util.implementations.injection.cditcktests.inheritance.generics;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class MemberLevelInheritanceTest {

    @Test
    void testInjectionPointDefinition() {
        Syringe syringe = newSyringe();
        boolean activatedRequest = syringe.activateRequestContextIfNeeded();
        try {
            Bean<Foo> fooBean = resolveBean(syringe.getBeanManager(), Foo.class);
            Set<InjectionPoint> injectionPoints = fooBean.getInjectionPoints();
            assertEquals(4, injectionPoints.size());

            for (InjectionPoint injectionPoint : injectionPoints) {
                if ("baz".equals(injectionPoint.getMember().getName())) {
                    assertEquals(new TypeLiteral<Baz<String>>() {
                    }.getType(), injectionPoint.getType());
                } else if ("t1".equals(injectionPoint.getMember().getName())) {
                    assertEquals(String.class, injectionPoint.getType());
                } else if ("t2BazList".equals(injectionPoint.getMember().getName())) {
                    assertEquals(new TypeLiteral<Baz<List<Qux>>>() {
                    }.getType(), injectionPoint.getType());
                } else if ("setT1Array".equals(injectionPoint.getMember().getName())) {
                    assertEquals(String[].class, injectionPoint.getType());
                } else {
                    fail("Unexpected injection point");
                }
            }
        } finally {
            if (activatedRequest) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testInjectionPoint() {
        Syringe syringe = newSyringe();
        boolean activatedRequest = syringe.activateRequestContextIfNeeded();
        try {
            Foo foo = getContextualReference(syringe.getBeanManager(), Foo.class);
            assertNotNull(foo);
            assertNotNull(foo.getBaz());
            assertNotNull(foo.getT1Array());
            assertNotNull(foo.getT2BazList());
        } finally {
            if (activatedRequest) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    @Test
    void testObserverResolution() {
        Syringe syringe = newSyringe();
        try {
            Set<ObserverMethod<? super Qux>> observerMethods = syringe.getBeanManager().resolveObserverMethods(new Qux(null));
            assertEquals(1, observerMethods.size());
            ObserverMethod<? super Qux> observerMethod = observerMethods.iterator().next();
            assertEquals(Foo.class, observerMethod.getBeanClass());
            assertEquals(new TypeLiteral<Baz<String>>() {
            }.getType(), observerMethod.getObservedType());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testObserver() {
        Syringe syringe = newSyringe();
        boolean activatedRequest = syringe.activateRequestContextIfNeeded();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Foo foo = getContextualReference(beanManager, Foo.class);
            assertNotNull(foo);
            beanManager.getEvent().select(Qux.class).fire(new Qux(null));
            assertNotNull(foo.getT1BazEvent());
            assertEquals("ok", foo.getT1ObserverInjectionPoint());
        } finally {
            if (activatedRequest) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Amazing.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bar.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Baz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Producer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Qux.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = (Set) beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
