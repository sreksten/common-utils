package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition.parameters;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisposedParameterPositionTest {

    @Test
    void testDisposedParameterPosition() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Idea.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Thinker.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IdeaFactory.class, BeanArchiveMode.EXPLICIT);
        syringe.start();

        try {
            BeanManager beanManager = syringe.getBeanManager();
            Thinker thinker = resolveReference(beanManager, Thinker.class);

            assertEquals(0, thinker.getIdeas().size());

            Bean<Idea> bean = resolveBean(beanManager, Idea.class);
            CreationalContext<Idea> ctx = beanManager.createCreationalContext(bean);
            Idea instance = bean.create(ctx);

            assertEquals(1, thinker.getIdeas().size());

            bean.destroy(instance, ctx);
            assertEquals(0, thinker.getIdeas().size());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        Set<Bean<?>> beans = beanManager.getBeans(type);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<T> bean = resolveBean(beanManager, type);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
