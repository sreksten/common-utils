package com.threeamigos.common.util.implementations.injection.cditcktests.full.context.passivating.custom;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomPassivatingScopeCalledWithSerializableParametersTest {

    @Test
    void testWithImplicitBean() {
        ClusteringExtension clusteringExtension = new ClusteringExtension();
        Syringe syringe = newSyringe(clusteringExtension, new BarExtension());
        try {
            ClusterContext context = clusteringExtension.getContext();
            context.reset();
            Foo instance = getContextualReference(syringe.getBeanManager(), Foo.class);
            assertNotNull(instance);
            assertEquals("pong", instance.ping());
            assertTrue(context.isGetCalled());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testWithExtensionProvidedBean() {
        ClusteringExtension clusteringExtension = new ClusteringExtension();
        Syringe syringe = newSyringe(clusteringExtension, new BarExtension());
        try {
            ClusterContext context = clusteringExtension.getContext();
            context.reset();
            Bar instance = getContextualReference(syringe.getBeanManager(), Bar.class);
            assertNotNull(instance);
            assertEquals("pong", instance.ping());
            assertTrue(context.isGetCalled());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe(ClusteringExtension clusteringExtension, BarExtension barExtension) {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Bar.class,
                Foo.class,
                ClusterScoped.class,
                ClusterContext.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(clusteringExtension);
        syringe.addExtension(barExtension);
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
