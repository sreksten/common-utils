package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.annotated;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessAnnotatedTypeTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        ProcessAnnotatedTypeObserver.getAnnotatedclasses().clear();

        syringe = new Syringe(new InMemoryMessageHandler());
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessAnnotatedTypeObserver.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(AbstractC.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ClassD.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Dog.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(InterfaceA.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(InterfaceB.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Mammal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Type.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(VetoedBean.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testProcessAnnotatedTypeEventsSent() {
        assertTrue(ProcessAnnotatedTypeObserver.getAnnotatedclasses().contains(AbstractC.class));
        assertTrue(ProcessAnnotatedTypeObserver.getAnnotatedclasses().contains(ClassD.class));
        assertTrue(ProcessAnnotatedTypeObserver.getAnnotatedclasses().contains(Dog.class));
        assertTrue(ProcessAnnotatedTypeObserver.getAnnotatedclasses().contains(InterfaceA.class));
    }

    @Test
    void testProcessAnnotatedTypeFiredForEnum() {
        assertTrue(ProcessAnnotatedTypeObserver.getAnnotatedclasses().contains(Type.class));
    }

    @Test
    void testGetAnnotatedType() {
        AnnotatedType<Dog> annotatedType = ProcessAnnotatedTypeObserver.getDogAnnotatedType();
        assertEquals(Dog.class, annotatedType.getBaseType());
        Set<AnnotatedMethod<? super Dog>> annotatedMethods = annotatedType.getMethods();
        assertEquals(3, annotatedMethods.size());

        Set<String> validMethodNames = new HashSet<String>(Arrays.asList("bite", "live", "drinkMilk"));
        for (AnnotatedMethod<? super Dog> annotatedMethod : annotatedMethods) {
            if (!validMethodNames.contains(annotatedMethod.getJavaMember().getName())) {
                fail("Invalid method name found " + annotatedMethod.getJavaMember().getName());
            }
        }
    }

    @Test
    void testSetAnnotatedType() {
        assertTrue(TestAnnotatedType.isGetConstructorsUsed());
        assertTrue(TestAnnotatedType.isGetFieldsUsed());
        assertTrue(TestAnnotatedType.isGetMethodsUsed());
    }

    @Test
    void testVeto() {
        assertTrue(beanManager.getBeans(VetoedBean.class).isEmpty());
    }
}
