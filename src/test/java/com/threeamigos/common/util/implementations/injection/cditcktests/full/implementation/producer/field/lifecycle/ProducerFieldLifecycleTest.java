package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.producer.field.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProducerFieldLifecycleTest {

    @Test
    void testProducerFieldFromSpecializingBeanUsed() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Animal.class,
                DeadlyAnimal.class,
                DeadlySpider.class,
                DefangedTarantula.class,
                SpecializedTarantulaProducer.class,
                Spider.class,
                Tame.class,
                Tarantula.class,
                TarantulaConsumer.class,
                TarantulaProducer.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            syringe.setup();
            BeanManager beanManager = syringe.getBeanManager();
            TarantulaConsumer spiderConsumer = getContextualReference(beanManager, TarantulaConsumer.class);
            assertNotNull(spiderConsumer.getConsumedTarantula());
            assertTrue(spiderConsumer.getConsumedTarantula() instanceof DefangedTarantula);
        } finally {
            syringe.shutdown();
        }
    }

    private <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        return type.cast(beanManager.getReference(resolved, type, beanManager.createCreationalContext(resolved)));
    }
}
