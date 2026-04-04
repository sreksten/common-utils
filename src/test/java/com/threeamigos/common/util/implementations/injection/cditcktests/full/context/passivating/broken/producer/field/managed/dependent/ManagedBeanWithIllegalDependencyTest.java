package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.broken.producer.field.managed.dependent;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedBeanWithIllegalDependencyTest {

    @Test
    void testFieldInjectionPointRequiringPassivationCapableDependency() {
        verify(FieldInjectionCorralBroken.class);
    }

    @Test
    void testSetterInjectionPointRequiringPassivationCapableDependency() {
        verify(SetterInjectionCorralBroken.class);
    }

    @Test
    void testConstructorInjectionPointRequiringPassivationCapableDependency() {
        verify(ConstructorInjectionCorralBroken.class);
    }

    private void verify(Class<? extends Corral> clazz) {
        Syringe syringe = newSyringe();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            String sessionId = "field-managed-dependent-" + clazz.getSimpleName() + "-" + UUID.randomUUID();
            beanManager.getContextManager().activateSession(sessionId);
            try {
                assertThrows(IllegalProductException.class, () -> getContextualReference(beanManager, clazz).ping());
            } finally {
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                British.class,
                ConstructorInjectionCorralBroken.class,
                Corral.class,
                Cow.class,
                CowProducer.class,
                FieldInjectionCorralBroken.class,
                SetterInjectionCorralBroken.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> beanType) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }
}
