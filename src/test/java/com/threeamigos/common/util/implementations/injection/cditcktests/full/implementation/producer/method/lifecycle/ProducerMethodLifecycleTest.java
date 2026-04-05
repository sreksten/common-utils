package com.threeamigos.common.util.implementations.injection.cditcktests.full.implementation.producer.method.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Isolated
class ProducerMethodLifecycleTest {

    @Test
    void testProducerMethodFromSpecializedBeanUsed() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Animal.class,
                DeadlyAnimal.class,
                DeadlySpider.class,
                Null.class,
                Pet.class,
                PreferredSpiderProducer.class,
                Spider.class,
                SpiderProducer.class,
                Tarantula.class,
                Web.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        try {
            syringe.setup();
            SpiderProducer.reset();
            PreferredSpiderProducer.reset();

            BeanManager beanManager = syringe.getBeanManager();
            Bean<Tarantula> spiderBean = getUniqueBean(beanManager, Tarantula.class, new Pet.Literal());
            CreationalContext<Tarantula> spiderBeanCc = beanManager.createCreationalContext(spiderBean);
            Tarantula tarantula = spiderBean.create(spiderBeanCc);

            assertEquals(PreferredSpiderProducer.getTarantulaCreated(), tarantula);
            assertFalse(SpiderProducer.isTarantulaCreated());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        return (Bean<T>) resolved;
    }
}
