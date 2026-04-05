package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtensionLifecycleTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private SimpleBean simpleBean;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(new InMemoryMessageHandler(), ExtensionLifecycleTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(SimpleExtension.class.getName());
        syringe.setup();

        beanManager = syringe.getBeanManager();
        simpleBean = beanManager.createInstance().select(SimpleBean.class).get();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testContainerProvidesBeanForExtension() {
        Set<Bean<?>> beans = beanManager.getBeans(SimpleExtension.class);
        assertEquals(1, beans.size());

        Bean<?> simpleExtensionBean = beans.iterator().next();
        assertEquals(ApplicationScoped.class, simpleExtensionBean.getScope());

        assertEquals(2, simpleExtensionBean.getQualifiers().size());
        assertTrue(simpleExtensionBean.getQualifiers().contains(Default.Literal.INSTANCE));
        assertTrue(simpleExtensionBean.getQualifiers().contains(Any.Literal.INSTANCE));

        Set<Type> types = simpleExtensionBean.getTypes();
        assertEquals(4, types.size());
        assertTrue(types.contains(SuperExtension.class));
        assertTrue(types.contains(Extension.class));
        assertTrue(types.contains(SimpleExtension.class));
        assertTrue(types.contains(Object.class));
    }

    @Test
    void testContainerInstantiatesSingleInstanceOfExtension() {
        long id = simpleBean.getSimpleExtension().getId();
        assertEquals(id, beanManager.createInstance().select(SimpleExtension.class).get().getId());
    }

    @Test
    void testContainerDeliversEventNotifications() {
        assertTrue(simpleBean.getSimpleExtension().isContainerEventObserved());
        beanManager.getEvent().select(SimpleEvent.class).fire(new SimpleEvent(System.currentTimeMillis()));
        assertTrue(simpleBean.getSimpleExtension().isSimpleEventObserved());
    }
}
