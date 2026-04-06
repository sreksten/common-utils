package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.tests.contract.interceptorLifeCycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorLifeCycleTest {

    @Test
    void testInterceptorMethodsCalledAfterDependencyInjection() {
        resetBazLifecycleState();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            createCallAndDestroyBazInstance(syringe);
            // Assertions checking dependency injection are made in interceptors.
            assertTrue(AroundInvokeInterceptor.called);
            assertTrue(PostConstructInterceptor.called);
            assertTrue(PreDestroyInterceptor.called);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorInstanceCreatedWhenTargetInstanceCreated() {
        resetWarriorLifecycleState();
        Syringe syringe = newSyringe();
        try {
            syringe.setup();
            Instance<Warrior> instance = syringe.getBeanManager().createInstance().select(Warrior.class);
            for (int i = 1; i < 3; i++) {
                createWarriorInstanceAndAssertInterceptorsCount(instance, i);
            }
        } finally {
            syringe.shutdown();
        }
    }

    private void createWarriorInstanceAndAssertInterceptorsCount(Instance<Warrior> instance, int repetition) {
        Warrior warrior = instance.get();
        assertEquals(repetition, WarriorPCInterceptor.count);
        assertEquals(repetition, WarriorPDInterceptor.count);
        assertEquals(repetition, WarriorAIInterceptor.count);
        assertEquals(repetition, MethodInterceptor.count);
        assertEquals(repetition, WarriorAttackAIInterceptor.count);
        // Two weapons are injected into warrior.
        assertEquals(2 * repetition, WeaponAIInterceptor.count);

        // When warrior attacks, he uses his weapon - at that time
        // an instance of WeaponAIInterceptor intercepting its usage is set to its field.
        warrior.attack1();
        warrior.attack2();
        assertNotEquals(warrior.getWeapon1().getWI(), warrior.getWeapon2().getWI());
        assertEquals(repetition, WarriorAIInterceptor.count);
        assertEquals(repetition, MethodInterceptor.count);
        assertEquals(repetition, WarriorAttackAIInterceptor.count);
    }

    private void createCallAndDestroyBazInstance(Syringe syringe) {
        Class<Baz> clazz = Baz.class;
        Bean<Baz> bean = getUniqueBean(syringe, clazz);
        CreationalContext<Baz> ctx = syringe.getBeanManager().createCreationalContext(bean);
        Baz instance = clazz.cast(syringe.getBeanManager().getReference(bean, clazz, ctx));
        instance.doSomething();
        bean.destroy(instance, ctx);
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AroundInvokeInterceptor.class,
                AttackBinding.class,
                Bar.class,
                Baz.class,
                BazBinding.class,
                MethodBinding.class,
                MethodInterceptor.class,
                PostConstructInterceptor.class,
                PreDestroyInterceptor.class,
                Warrior.class,
                WarriorAIInterceptor.class,
                WarriorAttackAIInterceptor.class,
                WarriorBinding.class,
                WarriorPCInterceptor.class,
                WarriorPDInterceptor.class,
                Weapon.class,
                WeaponAIInterceptor.class,
                WeaponBinding.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        return syringe;
    }

    private <T> Bean<T> getUniqueBean(Syringe syringe, Class<T> type) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(type);
        Bean<?> bean = syringe.getBeanManager().resolve(beans);
        assertNotNull(bean);
        @SuppressWarnings("unchecked")
        Bean<T> cast = (Bean<T>) bean;
        return cast;
    }

    private static void resetBazLifecycleState() {
        AroundInvokeInterceptor.called = false;
        PostConstructInterceptor.called = false;
        PreDestroyInterceptor.called = false;
    }

    private static void resetWarriorLifecycleState() {
        WarriorPCInterceptor.count = 0;
        WarriorPDInterceptor.count = 0;
        WarriorAIInterceptor.count = 0;
        MethodInterceptor.count = 0;
        WarriorAttackAIInterceptor.count = 0;
        WeaponAIInterceptor.count = 0;
    }
}
