package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.producer.method.definition;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProducerMethodDefinitionTest {

    @Test
    void testNonStaticProducerMethodNotInheritedBySpecializingSubclass() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AndalusianChicken.class,
                Chicken.class,
                Egg.class,
                Yummy.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            syringe.setup();
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(0, beanManager.getBeans(Egg.class, new Yummy.Literal()).size());
            assertThrows(UnsatisfiedResolutionException.class,
                    () -> getContextualReference(beanManager, Egg.class, new Yummy.Literal()));
        } finally {
            syringe.shutdown();
        }
    }

    private <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        if (beans.isEmpty()) {
            throw new UnsatisfiedResolutionException("No bean found for type " + type.getName());
        }
        Bean<?> resolved = beanManager.resolve(beans);
        if (resolved == null) {
            throw new UnsatisfiedResolutionException("No resolvable bean for type " + type.getName());
        }
        return type.cast(beanManager.getReference(resolved, type, beanManager.createCreationalContext(resolved)));
    }
}
