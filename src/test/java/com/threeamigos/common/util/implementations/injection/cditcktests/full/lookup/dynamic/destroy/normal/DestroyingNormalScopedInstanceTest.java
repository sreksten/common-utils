package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.dynamic.destroy.normal;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class DestroyingNormalScopedInstanceTest {

    private static final String[] VALUES = {"foo", "bar", "baz"};

    @Test
    void testCustomScopedComponent() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Instance<AlterableComponent> instance = syringe.getBeanManager().createInstance().select(AlterableComponent.class);
            testComponent(instance);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testUnsupportedOperationExceptionThrownIfUnderlyingContextNotAlterable() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Instance<NonAlterableComponent> instance = syringe.getBeanManager().createInstance().select(NonAlterableComponent.class);
            NonAlterableComponent component = instance.get();
            assertThrows(UnsupportedOperationException.class, () -> instance.destroy(component));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testContextDestroyCalled() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Instance<AlterableComponent> instance = syringe.getBeanManager().createInstance().select(AlterableComponent.class);
            AlterableComponent component = instance.get();
            CustomAlterableContext.reset();
            instance.destroy(component);
            assertTrue(CustomAlterableContext.isDestroyCalled());
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AbstractComponent.class,
                AbstractContext.class,
                AlterableComponent.class,
                AlterableScoped.class,
                CustomAlterableContext.class,
                CustomNonAlterableContext.class,
                CustomScopeExtension.class,
                NonAlterableComponent.class,
                NonAlterableScoped.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(CustomScopeExtension.class.getName());
        return syringe;
    }

    private <T extends AbstractComponent> void testComponent(Instance<T> instance) {
        for (String string : VALUES) {
            T reference = instance.get();
            assertNull(reference.getValue());
            reference.setValue(string);
            assertEquals(string, reference.getValue());

            AbstractComponent.reset();
            instance.destroy(reference);
            assertTrue(AbstractComponent.isDestroyed());
            assertNull(reference.getValue(), reference.getValue());
        }
    }
}
