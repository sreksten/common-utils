package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.lifecycleCallback.bindings;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class LifecycleInterceptorDefinitionTest {

    @Test
    void testLifecycleInterception() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            ActionSequence.reset();

            Bean<Missile> bean = getUniqueBean(syringe, Missile.class);
            CreationalContext<Missile> ctx = syringe.getBeanManager().createCreationalContext(bean);
            Missile missile = bean.create(ctx);
            missile.fire();
            bean.destroy(missile, ctx);

            assertEquals(1, ActionSequence.getSequenceSize("postConstruct"));
            assertEquals(AirborneInterceptor.class.getSimpleName(), ActionSequence.getSequenceData("postConstruct").get(0));
            assertEquals(1, ActionSequence.getSequenceSize("preDestroy"));
            assertEquals(AirborneInterceptor.class.getSimpleName(), ActionSequence.getSequenceData("preDestroy").get(0));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testMultipleLifecycleInterceptors() {
        Syringe syringe = newSyringe();
        try {
            syringe.setup();

            ActionSequence.reset();

            Bean<Rocket> bean = getUniqueBean(syringe, Rocket.class);
            CreationalContext<Rocket> ctx = syringe.getBeanManager().createCreationalContext(bean);
            Rocket rocket = bean.create(ctx);
            rocket.fire();
            bean.destroy(rocket, ctx);

            ActionSequence postConstruct = ActionSequence.getSequence("postConstruct");
            postConstruct.assertDataEquals(AirborneInterceptor.class, SuperDestructionInterceptor.class,
                    DestructionInterceptor.class, Weapon.class, Rocket.class);

            ActionSequence preDestroy = ActionSequence.getSequence("preDestroy");
            preDestroy.assertDataEquals(AirborneInterceptor.class, SuperDestructionInterceptor.class,
                    DestructionInterceptor.class, Weapon.class, Rocket.class);

            ActionSequence aroundConstruct = ActionSequence.getSequence("aroundConstruct");
            aroundConstruct.assertDataEquals(AirborneInterceptor.class, SuperDestructionInterceptor.class,
                    DestructionInterceptor.class);
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Airborne.class,
                AirborneInterceptor.class,
                DestructionInterceptor.class,
                Destructive.class,
                Foo.class,
                Missile.class,
                Rocket.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getUniqueBean(Syringe syringe, Class<T> beanClass) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(beanClass);
        assertEquals(1, beans.size());
        return (Bean<T>) beans.iterator().next();
    }
}
