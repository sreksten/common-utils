package com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.discovery;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeanDiscoveryTest {

    private Syringe syringe;
    private VerifyingExtension extension;
    private BeanManagerImpl beanManager;

    @BeforeAll
    void setUp() {
        extension = new VerifyingExtension();

        syringe = new Syringe();
        syringe.addExtension(extension);
        syringe.addExtension(ScopesExtension.class.getName());
        syringe.addExtension(LegacyExtension.class.getName());

        syringe.initialize();
        // Explicit bean archives (bean-discovery-mode="all")
        syringe.addDiscoveredClass(Alpha.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Alpha2.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Alpha3.class, BeanArchiveMode.EXPLICIT);

        // Implicit bean archives (annotated/default descriptor behavior)
        syringe.addDiscoveredClass(Bravo.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Charlie.class, BeanArchiveMode.IMPLICIT);

        syringe.addDiscoveredClass(Delta.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Golf.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(India.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Kilo.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Mike.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Interceptor1.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Decorator1.class, BeanArchiveMode.IMPLICIT);

        syringe.addDiscoveredClass(Echo.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(EchoNotABean.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Hotel.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Juliet.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(JulietNotABean.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Lima.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(November.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Interceptor2.class, BeanArchiveMode.IMPLICIT);
        syringe.addDiscoveredClass(Decorator2.class, BeanArchiveMode.IMPLICIT);

        // Bean archive mode "none"
        syringe.addDiscoveredClass(Foxtrot.class, BeanArchiveMode.NONE);
        syringe.addDiscoveredClass(LegacyBravo.class, BeanArchiveMode.NONE);

        syringe.start();

        beanManager = (BeanManagerImpl) syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testExplicitBeanArchiveModeAll() {
        assertDiscoveredAndAvailable(Alpha.class);
    }

    @Test
    void testExplicitBeanArchiveModeAllVersion11() {
        assertDiscoveredAndAvailable(Alpha2.class);
    }

    @Test
    void testExplicitBeanArchiveModeAllVersion20() {
        assertDiscoveredAndAvailable(Alpha3.class);
    }

    @Test
    void testImplicitBeanArchiveEmptyDescriptor() {
        assertDiscoveredAndAvailable(Bravo.class);
    }

    @Test
    void testArchiveWithNoVersionBeansXml() {
        assertDiscoveredAndAvailable(Charlie.class);
    }

    @Test
    void testNormalScopeImplicitBeanArchiveNoDescriptor() {
        assertDiscoveredAndAvailable(Delta.class);
        assertDiscoveredAndAvailable(Golf.class);
    }

    @Test
    void testNormalScopeImplicitBeanArchiveModeAnnotated() {
        assertDiscoveredAndAvailable(Echo.class);
        assertNotDiscoveredAndNotAvailable(EchoNotABean.class);
        assertDiscoveredAndAvailable(Hotel.class);
    }

    @Test
    void testDependentScopeImplicitBeanArchiveNoDescriptor() {
        assertDiscoveredAndAvailable(India.class);
    }

    @Test
    void testPseudoScopeImplicitBeanArchiveModeAnnotated() {
        assertDiscoveredAndAvailable(Juliet.class);
        assertNotDiscoveredAndNotAvailable(JulietNotABean.class);
    }

    @Test
    void testInterceptorIsBeanDefiningAnnotation() {
        assertDiscovered(Interceptor1.class);
        assertDiscovered(Interceptor2.class);
    }

    @Test
    void testDecoratorIsBeanDefiningAnnotation() {
        assertDiscovered(Decorator1.class);
        assertDiscovered(Decorator2.class);
    }

    @Test
    void testStereotypeImplicitBeanArchiveNoDescriptor() {
        assertDiscoveredAndAvailable(Mike.class);
        assertDiscovered(Kilo.class);
    }

    @Test
    void testStereotypeImplicitBeanArchiveModeAnnotated() {
        assertDiscoveredAndAvailable(November.class);
        assertDiscovered(Lima.class);
    }

    @Test
    void testNoBeanArchiveModeNone() {
        assertNotDiscoveredAndNotAvailable(Foxtrot.class);
    }

    @Test
    void testNotBeanArchiveExtension() {
        assertDiscoveredAndAvailable(LegacyAlpha.class);
        assertNotDiscoveredAndNotAvailable(LegacyBravo.class);
    }

    private <T extends Ping> void assertDiscoveredAndAvailable(Class<T> clazz) {
        assertDiscovered(clazz);
        Bean<T> bean = getUniqueBean(clazz);
        T reference = clazz.cast(beanManager.getReference(bean, clazz, beanManager.createCreationalContext(bean)));
        assertNotNull(reference);
        if (RequestScoped.class.equals(bean.getScope())) {
            beanManager.getContextManager().activateRequest();
            try {
                reference.pong();
            } finally {
                beanManager.getContextManager().deactivateRequest();
            }
        } else {
            reference.pong();
        }
        assertNotNull(bean);
    }

    private void assertDiscovered(Class<?> clazz) {
        assertTrue(extension.getObservedAnnotatedTypes().contains(clazz), clazz.getSimpleName() + " not discovered.");
    }

    private <T> void assertNotDiscoveredAndNotAvailable(Class<T> clazz) {
        assertFalse(extension.getObservedAnnotatedTypes().contains(clazz));
        assertTrue(beanManager.getBeans(clazz).isEmpty());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private <T extends Ping> T getContextualReference(Class<T> type) {
        Bean<T> bean = getUniqueBean(type);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
