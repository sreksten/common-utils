package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.bbd;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeploymentTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        ManagerObserver.reset();

        syringe = new Syringe(new InMemoryMessageHandler(), DeploymentTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ManagerObserver.class.getName());
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
    void testDeployedManagerEvent() {
        assertTrue(ManagerObserver.isAfterDeploymentValidationCalled());
        assertTrue(ManagerObserver.isAfterBeanDiscoveryCalled());
    }

    @Test
    void testOnlyEnabledBeansDeployed() {
        assertFalse(getBeans(User.class).isEmpty());
        assertTrue(getBeans(DataAccessAuthorizationDecorator.class).isEmpty());
        assertTrue(getBeans(Interceptor1.class).isEmpty());
        assertTrue(getBeans(DisabledBean.class).isEmpty());
    }

    private Set<Bean<?>> getBeans(Class<?> type) {
        return beanManager.getBeans(type);
    }
}
