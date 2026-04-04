package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.injection.point;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PassivationCapableInjectionPointTest {

    @Test
    void testPassivationCapableInjectionPoints() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Meal.class, Spoon.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
            String sessionId = "passivating-injection-point-" + UUID.randomUUID();
            beanManager.getContextManager().activateSession(sessionId);
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                Spoon spoon = getContextualReference(beanManager, Spoon.class);
                assertNotNull(spoon.getMeal1());
                assertNotNull(spoon.getMeal2());
                assertNotNull(spoon.getMeal3());
                assertEquals(spoon.getMeal1().getId(), spoon.getMeal2().getId());
                assertEquals(spoon.getMeal1().getId(), spoon.getMeal3().getId());
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
                beanManager.getContextManager().invalidateSession(sessionId);
            }
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> beanType) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }
}
