package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.bean.alternativePriority;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeanConfiguratorAlternativePriorityTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AlternativePriorityExtension.class,
                Bar.class,
                Foo.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AlternativePriorityExtension.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testSyntheticAlternativeIsEnabled() {
        AlternativePriorityExtension extension = beanManager.getExtension(AlternativePriorityExtension.class);
        assertTrue(extension.isSyntheticAlternativeProcessed());

        Foo foo = getContextualReference(Foo.class);
        Bar bar = getContextualReference(Bar.class);
        assertEquals("bar", foo.ping());
        assertEquals("bar", bar.ping());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T getContextualReference(Class<T> beanType, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType, qualifiers);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return beanType.cast(beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean)));
    }
}
