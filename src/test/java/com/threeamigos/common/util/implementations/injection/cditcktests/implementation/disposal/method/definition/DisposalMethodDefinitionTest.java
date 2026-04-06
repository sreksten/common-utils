package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class DisposalMethodDefinitionTest {

    private static final Annotation DEADLIEST_LITERAL = new Deadliest.Literal();
    private static final Annotation TAME_LITERAL = new Tame.Literal();

    @BeforeEach
    void resetState() {
        SpiderProducer.reset();
        DisposalNonBean.setWebSpiderdestroyed(false);
    }

    @Test
    void testBindingTypesAppliedToDisposalMethodParameters() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            assertFalse(SpiderProducer.isTameSpiderDestroyed());
            assertFalse(SpiderProducer.isDeadliestTarantulaDestroyed());

            createAndDestroyBean(beanManager, Tarantula.class, DEADLIEST_LITERAL);

            assertTrue(SpiderProducer.isTameSpiderDestroyed());
            assertTrue(SpiderProducer.isDeadliestTarantulaDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposalMethodOnNonBean() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            DependentInstance<WebSpider> webSpider = newDependentInstance(beanManager, WebSpider.class, DEADLIEST_LITERAL);
            WebSpider instance = webSpider.get();
            assertNotNull(instance);
            webSpider.destroy();

            assertFalse(DisposalNonBean.isWebSpiderdestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposalMethodParametersGetInjected() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            DependentInstance<SandSpider> sandSpider = newDependentInstance(beanManager, SandSpider.class, DEADLIEST_LITERAL);
            SandSpider sandSpiderInst = sandSpider.get();
            assertNotNull(sandSpiderInst);
            sandSpider.destroy();

            assertTrue(SpiderProducer.isDeadliestSandSpiderDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposalMethodForMultipleProducerMethods() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            DependentInstance<Widow> deadliest = newDependentInstance(beanManager, Widow.class, DEADLIEST_LITERAL);
            Widow deadliestInstance = deadliest.get();
            assertNotNull(deadliestInstance);
            deadliest.destroy();

            DependentInstance<Widow> tame = newDependentInstance(beanManager, Widow.class, TAME_LITERAL);
            Widow tameInstance = tame.get();
            assertNotNull(tameInstance);
            tame.destroy();

            assertEquals(2, SpiderProducer.getWidowsDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDisposalMethodCalledForProducerField() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();

            createAndDestroyBean(beanManager, Calisoga.class, new Scary.Literal());
            assertTrue(SpiderProducer.isScaryBlackWidowDestroyed());
            assertFalse(SpiderProducer.isTameBlackWidowDestroyed());

            SpiderProducer.reset();
            createAndDestroyBean(beanManager, Calisoga.class, TAME_LITERAL);
            assertFalse(SpiderProducer.isScaryBlackWidowDestroyed());
            assertTrue(SpiderProducer.isTameBlackWidowDestroyed());
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Tarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WebSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SandSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Widow.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Calisoga.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DisposalNonBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Deadliest.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Scary.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    private static <T> void createAndDestroyBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        T instance = bean.create(creationalContext);
        bean.destroy(instance, creationalContext);
    }

    private static <T> DependentInstance<T> newDependentInstance(BeanManager beanManager, Class<T> type,
            Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        T instance = bean.create(creationalContext);
        return new DependentInstance<T>(bean, creationalContext, instance);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static class DependentInstance<T> {

        private final Bean<T> bean;
        private final CreationalContext<T> creationalContext;
        private final T instance;

        private DependentInstance(Bean<T> bean, CreationalContext<T> creationalContext, T instance) {
            this.bean = bean;
            this.creationalContext = creationalContext;
            this.instance = instance;
        }

        private T get() {
            return instance;
        }

        private void destroy() {
            bean.destroy(instance, creationalContext);
        }
    }
}
