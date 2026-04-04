package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.alterable;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlterableContextTest {

    private static final String[] VALUES = {"foo", "bar", "baz"};

    @Test
    void testCustomScopedComponent() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AbstractComponent.class,
                CustomScoped.class,
                CustomScopedComponent.class,
                CustomContext.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(CustomScopeExtension.class.getName());
        syringe.setup();
        try {
            testComponent(syringe.getBeanManager(), CustomScopedComponent.class);
        } finally {
            syringe.shutdown();
        }
    }

    private <T extends AbstractComponent> void testComponent(BeanManager beanManager, Class<T> javaClass) {
        Bean<?> bean = resolveUniqueBean(beanManager, javaClass);
        @SuppressWarnings("unchecked")
        T reference = (T) beanManager.getReference(bean, javaClass, beanManager.createCreationalContext(bean));
        AlterableContext context = (AlterableContext) beanManager.getContext(bean.getScope());

        for (String string : VALUES) {
            assertNull(reference.getValue());
            reference.setValue(string);
            assertEquals(string, reference.getValue());

            AbstractComponent.reset();
            context.destroy(bean);
            assertTrue(AbstractComponent.isDestroyed());
            assertNull(reference.getValue(), reference.getValue());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> resolveUniqueBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }
}
