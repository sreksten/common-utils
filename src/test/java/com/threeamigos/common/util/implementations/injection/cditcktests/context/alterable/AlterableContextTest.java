package com.threeamigos.common.util.implementations.injection.cditcktests.context.alterable;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.alterable.alterablecontexttest.testcomponent.AbstractComponent;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.alterable.alterablecontexttest.testcomponent.ApplicationScopedComponent;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.alterable.alterablecontexttest.testcomponent.RequestScopedComponent;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlterableContextTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.alterable.alterablecontexttest.testcomponent";

    private static final String[] VALUES = {"foo", "bar", "baz"};

    @Test
    void testApplicationScopedComponent() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            testComponent(syringe.getBeanManager(), ApplicationScopedComponent.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testRequestScopedComponent() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                testComponent(syringe.getBeanManager(), RequestScopedComponent.class);
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
    void testNothingHappensIfNoInstanceToDestroy() {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            ApplicationScopedComponent application = resolveReference(beanManager, ApplicationScopedComponent.class);
            Bean<?> bean = resolveBean(beanManager, ApplicationScopedComponent.class);
            AlterableContext context = (AlterableContext) beanManager.getContext(bean.getScope());

            AbstractComponent.reset();
            application.setValue("value");
            context.destroy(bean);
            assertTrue(AbstractComponent.isDestroyed());

            context.destroy(bean);
            context.destroy(bean);
        } finally {
            syringe.shutdown();
        }
    }

    private <T extends AbstractComponent> void testComponent(BeanManager beanManager, Class<T> javaClass) {
        Bean<?> bean = resolveBean(beanManager, javaClass);
        @SuppressWarnings("unchecked")
        T reference = (T) beanManager.getReference(bean, javaClass, beanManager.createCreationalContext(bean));
        AlterableContext context = (AlterableContext) beanManager.getContext(bean.getScope());

        for (String string : VALUES) {
            assertNull(reference.getValue());
            reference.setValue(string);
            assertEquals(string, reference.getValue());

            AbstractComponent.reset();
            context.destroy(bean);
            assertTrue(AbstractComponent.isDestroyed());
            assertNull(reference.getValue());
        }
    }

    private static <T> T resolveReference(BeanManager beanManager, Class<T> type) {
        Bean<?> bean = resolveBean(beanManager, type);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type) {
        return (Bean<T>) beanManager.resolve((java.util.Set) beanManager.getBeans(type));
    }
}
