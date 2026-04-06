package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.producer.method.definition.name;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("serial")
class ProducerMethodWithDefaultNameTest {

    @Test
    void testMethodName() {
        Syringe syringe = startContainer();
        try {
            Bean<Bug> terry = getUniqueBean(syringe.getBeanManager(), Bug.class, new Crazy.Literal());
            assertEquals("findTerry", terry.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testJavaBeansPropertyName() {
        Syringe syringe = startContainer();
        try {
            Bean<Bug> graham = getUniqueBean(syringe.getBeanManager(), Bug.class);
            assertEquals("graham", graham.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerMethodQualifiers() {
        Syringe syringe = startContainer();
        try {
            Bean<Bug> john = getUniqueBean(syringe.getBeanManager(), Bug.class, new Funny.Literal());
            assertEquals("produceJohn", john.getName());
            assertEquals(2, john.getQualifiers().size());
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Bug.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BugProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BugStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Crazy.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Funny.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = (Set) beanManager.getBeans(type, qualifiers);
        assertEquals(1, beans.size());
        return beans.iterator().next();
    }
}
