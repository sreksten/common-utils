package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager.unmanaged;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Unmanaged;
import jakarta.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnmanagedInstanceTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Axe.class,
                Builder.class,
                Elephant.class,
                Hammer.class,
                Nail.class,
                Proboscis.class,
                ToolBinding.class,
                ToolInterceptor.class,
                Zoo.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        beanManager = syringe.getBeanManager();
        SyringeCDIProvider.registerThreadLocalCDI(syringe.getCDI());
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            try {
                SyringeCDIProvider.unregisterThreadLocalCDI();
            } finally {
                syringe.shutdown();
            }
        }
    }

    @Test
    void testObtainNonContextualInstanceUsingCurrentBeanManager() {
        Builder.reset();
        Nail.reset();
        Hammer.reset();

        Unmanaged<Builder> unmanagedBuilder = new Unmanaged<Builder>(beanManager, Builder.class);
        UnmanagedInstance<Builder> unmanagedBuilderInstance = unmanagedBuilder.newInstance();
        Builder builder = unmanagedBuilderInstance.produce().inject().postConstruct().get();
        builder.build();

        assertTrue(Builder.postConstructCalled);
        assertTrue(Nail.postConstructCalled);
        assertTrue(Hammer.postConstructCalled);

        unmanagedBuilderInstance.preDestroy().dispose();

        assertTrue(Builder.preDestroyCalled);
        assertTrue(Nail.preDestroyCalled);
        assertFalse(Hammer.preDestroyCalled);
    }

    @Test
    void testObtainNonContextualInstance() {
        Zoo.reset();
        Proboscis.reset();
        Elephant.reset();

        Unmanaged<Zoo> unmanagedZoo = new Unmanaged<Zoo>(Zoo.class);
        UnmanagedInstance<Zoo> unmanagedZooInstance = unmanagedZoo.newInstance();
        Zoo zoo = unmanagedZooInstance.produce().inject().postConstruct().get();
        zoo.build();

        assertTrue(Zoo.postConstructCalled);
        assertTrue(Proboscis.postConstructCalled);
        assertTrue(Elephant.postConstructCalled);

        unmanagedZooInstance.preDestroy().dispose();

        assertTrue(Zoo.preDestroyCalled);
        assertTrue(Proboscis.preDestroyCalled);
        assertFalse(Elephant.preDestroyCalled);
    }

    @Test
    void testNonContextualInstanceIsIntercepted() {
        ToolInterceptor.intercepted = false;

        Unmanaged<Axe> unmanagedAxe = new Unmanaged<Axe>(beanManager, Axe.class);
        UnmanagedInstance<Axe> unmanagedAxeInstance = unmanagedAxe.newInstance();
        unmanagedAxeInstance.produce().inject().postConstruct().get().cut();
        unmanagedAxeInstance.preDestroy().dispose();

        assertTrue(ToolInterceptor.intercepted);
    }
}
