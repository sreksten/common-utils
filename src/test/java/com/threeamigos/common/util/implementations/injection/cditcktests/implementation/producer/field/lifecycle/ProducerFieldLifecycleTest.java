package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.producer.field.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ProducerFieldLifecycleTest {

    private static final Annotation NULL_LITERAL = new Null.Literal();
    private static final Annotation BROKEN_LITERAL = new Broken.Literal();
    private static final Annotation TAME_LITERAL = new Tame.Literal();

    @Test
    void testProducerFieldNotAnotherBean() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertNotNull(getContextualReference(beanManager, BrownRecluse.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerStaticFieldBean() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            StaticTarantulaConsumer consumer = getContextualReference(beanManager, StaticTarantulaConsumer.class);
            assertEquals(StaticTarantulaProducer.produceTarantula, consumer.getConsumedTarantula());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerFieldBeanCreate() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            BlackWidowConsumer consumer = getContextualReference(beanManager, BlackWidowConsumer.class);
            assertEquals(BlackWidowProducer.blackWidow, consumer.getInjectedSpider());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerFieldReturnsNullIsDependent() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            NullSpiderConsumer consumer = getContextualReference(beanManager, NullSpiderConsumer.class);
            assertNull(consumer.getInjectedSpider());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerFieldForNullValueNotDependent() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<BlackWidow> spiderBean = resolveBean(beanManager, BlackWidow.class, NULL_LITERAL, BROKEN_LITERAL);
            final CreationalContext<BlackWidow> spiderContext = beanManager.createCreationalContext(spiderBean);
            assertThrows(IllegalProductException.class, new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() {
                    spiderBean.create(spiderContext);
                }
            });
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerFieldReturnsNullIsNotDependent() {
        Syringe syringe = startContainer();
        try {
            final BeanManager beanManager = syringe.getBeanManager();
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                assertThrows(IllegalProductException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        NullSpiderConsumerForBrokenProducer consumer = getContextualReference(beanManager,
                                NullSpiderConsumerForBrokenProducer.class);
                        if (consumer.getInjectedSpider() != null) {
                            consumer.getInjectedSpider().bite();
                        }
                    }
                });
            } finally {
                if (activatedRequest) {
                    syringe.deactivateRequestContextIfActive();
                }
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerFieldBeanDestroy() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            BlackWidowProducer.reset();
            Bean<BlackWidow> bean = resolveBean(beanManager, BlackWidow.class, TAME_LITERAL);
            CreationalContext<BlackWidow> ctx = beanManager.createCreationalContext(bean);
            BlackWidow instance = bean.create(ctx);
            bean.destroy(instance, ctx);
            assertTrue(BlackWidowProducer.blackWidowDestroyed);
            assertEquals(instance.getTimeOfBirth(), BlackWidowProducer.destroyedBlackWidowTimeOfBirth);
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BlackWidow.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BlackWidowConsumer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BlackWidowProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BrownRecluse.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BrownRecluseProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlyAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlySpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Null.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NullSpiderConsumer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NullSpiderConsumerForBrokenProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NullSpiderProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NullSpiderProducer_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Static.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StaticTarantulaConsumer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StaticTarantulaProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Working.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
