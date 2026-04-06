package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.producer.method.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ProducerMethodLifecycleTest {

    private static final Annotation PET_LITERAL = new Pet.Literal();
    private static final Annotation FIRST_BORN_LITERAL = new FirstBorn.Literal();
    private static final Annotation FAIL_LITERAL = new Fail.Literal();
    private static final Annotation NULL_LITERAL = new Null.Literal();

    @Test
    void testProducerMethodBeanCreate() {
        Syringe syringe = startContainer();
        try {
            SpiderProducer.reset();
            BeanManager beanManager = syringe.getBeanManager();
            Bean<Tarantula> tarantulaBean = resolveBean(beanManager, Tarantula.class, PET_LITERAL);
            CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(tarantulaBean);
            Tarantula tarantula = tarantulaBean.create(creationalContext);

            assertSame(SpiderProducer.getTarantulaCreated(), tarantula);
            assertNotNull(SpiderProducer.getInjectedWeb());
            assertTrue(SpiderProducer.getInjectedWeb().isDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerMethodInvokedOnCreate() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<SpiderEgg> eggBean = resolveBean(beanManager, SpiderEgg.class, FIRST_BORN_LITERAL);
            CreationalContext<SpiderEgg> eggCreationalContext = beanManager.createCreationalContext(eggBean);
            assertNotNull(eggBean.create(eggCreationalContext));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testWhenApplicationInvokesProducerMethodParametersAreNotInjected() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertThrows(AssertionError.class, () -> getContextualReference(beanManager, BrownRecluse.class).layAnEgg(null));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testCreateReturnsNullIfProducerDoesAndDependent() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<Spider> nullSpiderBean = resolveBean(beanManager, Spider.class, NULL_LITERAL);
            CreationalContext<Spider> nullSpiderCreationalContext = beanManager.createCreationalContext(nullSpiderBean);
            assertNull(nullSpiderBean.create(nullSpiderCreationalContext));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testCreateFailsIfProducerReturnsNullAndNotDependent() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<PotatoChip> potatoChipBean = resolveBean(beanManager, PotatoChip.class, NULL_LITERAL);
            CreationalContext<PotatoChip> chipCreationalContext = beanManager.createCreationalContext(potatoChipBean);
            assertThrows(IllegalProductException.class, () -> potatoChipBean.create(chipCreationalContext));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerMethodBeanDestroy() {
        Syringe syringe = startContainer();
        try {
            SpiderProducer.reset();
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> beans = beanManager.getBeans(Tarantula.class, PET_LITERAL);
            Bean<?> bean = beanManager.resolve(beans);
            assertEquals(SpiderProducer.class, bean.getBeanClass());
            assertTrue(bean.getTypes().contains(Tarantula.class));

            @SuppressWarnings("unchecked")
            Bean<Tarantula> tarantulaBean = (Bean<Tarantula>) bean;
            CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(tarantulaBean);
            Tarantula tarantula = tarantulaBean.create(creationalContext);

            SpiderProducer.resetInjections();
            tarantulaBean.destroy(tarantula, creationalContext);

            assertSame(SpiderProducer.getTarantulaDestroyed(), tarantula);
            assertTrue(SpiderProducer.isDestroyArgumentsSet());
            assertNotNull(SpiderProducer.getInjectedWeb());
            assertTrue(SpiderProducer.getInjectedWeb().isDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testCreateRethrowsUncheckedException() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<Ship> shipBean = resolveBean(beanManager, Ship.class, FAIL_LITERAL);
            CreationalContext<Ship> shipCreationalContext = beanManager.createCreationalContext(shipBean);
            assertThrows(FooException.class, () -> shipBean.create(shipCreationalContext));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testCreateWrapsCheckedExceptionAndRethrows() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<Lorry> lorryBean = resolveBean(beanManager, Lorry.class, FAIL_LITERAL);
            CreationalContext<Lorry> lorryCreationalContext = beanManager.createCreationalContext(lorryBean);
            assertThrows(CreationException.class, () -> lorryBean.create(lorryCreationalContext));
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BrownRecluse.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlyAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlySpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Fail.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FirstBorn.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FooException.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Lays.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Lorry.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LorryProducer_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Null.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Pet.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PotatoChip.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Ship.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ShipProducer_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderEgg.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Web.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = (Set) beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
