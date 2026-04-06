package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.simple.lifecycle.unproxyable;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.UnproxyableResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class UnproxyableManagedBeanTest {

    @Test
    void testNormalScopedUnproxyableBeanWithPrivateConstructorResolution() {
        assertUnproxyableResolution(UnproxyableBean.class);
    }

    @Test
    void testNormalScopedUnproxyableBeanWithFinalClassResolution() {
        assertUnproxyableResolution(UnproxyableFinalClass.class);
    }

    @Test
    void testNormalScopedUnproxyableBeanWithPublicFinalMethodResolution() {
        assertUnproxyableResolution(UnproxyableBeanWithPublicFinalMethod.class);
    }

    @Test
    void testNormalScopedUnproxyableBeanWithProtectedFinalMethodResolution() {
        assertUnproxyableResolution(UnproxyableBeanWithProtectedFinalMethod.class);
    }

    @Test
    void testNormalScopedUnproxyableBeanWithPackagePrivateFinalMethodResolution() {
        assertUnproxyableResolution(UnproxyableBeanWithPackagePrivateFinalMethod.class);
    }

    private <T> void assertUnproxyableResolution(Class<T> type) {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(type, BeanArchiveMode.EXPLICIT);
        try {
            syringe.start();
            BeanManager beanManager = syringe.getBeanManager();
            assertThrows(UnproxyableResolutionException.class, new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() {
                    Bean<T> bean = resolveBean(beanManager, type);
                    CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
                    beanManager.getReference(bean, type, creationalContext);
                }
            });
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = (Set) beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }
}
