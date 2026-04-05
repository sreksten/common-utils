package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.bean;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeanConfiguratorTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                BeanConfiguratorTest.class,
                Bogey.class,
                Dangerous.class,
                DesireToHurtHumans.class,
                Dungeon.class,
                Ghost.class,
                LifecycleObserver.class,
                Monster.class,
                MonsterController.class,
                Skeleton.class,
                Undead.class,
                Vampire.class,
                Weapon.class,
                Werewolf.class,
                Zombie.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(LifecycleObserver.class.getName());
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
    void testCreationalAndDisposalMethods() {
        Bean<Skeleton> skeletonBean = getUniqueBean(Skeleton.class, Undead.UndeadLiteral.INSTANCE);
        CreationalContext<Skeleton> skeletonCreationalContext = beanManager.createCreationalContext(skeletonBean);
        Skeleton skeleton = skeletonBean.create(skeletonCreationalContext);

        Bean<Zombie> zombieBean = getUniqueBean(Zombie.class, Undead.UndeadLiteral.INSTANCE,
                Dangerous.DangerousLiteral.INSTANCE);
        CreationalContext<Zombie> zombieCreationalContext = beanManager.createCreationalContext(zombieBean);
        Zombie zombie = zombieBean.create(zombieCreationalContext);

        spawnMonster(Ghost.class, Undead.UndeadLiteral.INSTANCE);
        spawnMonster(Vampire.class, Undead.UndeadLiteral.INSTANCE);

        assertTrue(MonsterController.skeletonProducerCalled);
        assertTrue(MonsterController.zombieProducerCalled);
        assertTrue(MonsterController.ghostInstanceObtained);
        assertTrue(MonsterController.vampireInstanceCreated);

        skeletonBean.destroy(skeleton, skeletonCreationalContext);
        zombieBean.destroy(zombie, zombieCreationalContext);
        assertTrue(MonsterController.zombieKilled);
        assertTrue(MonsterController.skeletonKilled);
    }

    @Test
    void testInjectionPoints() {
        Dungeon dungeon = getContextualReference(Dungeon.class);

        assertTrue(dungeon.hasAllMonters());

        assertEquals(1, getUniqueBean(Skeleton.class, Undead.UndeadLiteral.INSTANCE).getInjectionPoints().size());
        assertEquals(2, getUniqueBean(Zombie.class, Undead.UndeadLiteral.INSTANCE, Dangerous.DangerousLiteral.INSTANCE)
                .getInjectionPoints().size());

        Set<InjectionPoint> ghostIP = getUniqueBean(Ghost.class, Undead.UndeadLiteral.INSTANCE).getInjectionPoints();
        assertEquals(1, ghostIP.size());
        assertTrue(ghostIP.iterator().next().getAnnotated().getTypeClosure().contains(DesireToHurtHumans.class));
    }

    @Test
    void testPassivationCapability() {
        assertNotNull(beanManager.getPassivationCapableBean("zombie"));
    }

    @Test
    void testDefaultScopeOfAddedBean() {
        Bean<Bogey> bogeyBean = getUniqueBean(Bogey.class, Undead.UndeadLiteral.INSTANCE);
        assertEquals(Dependent.class, bogeyBean.getScope());

        Bean<Werewolf> werewolfBean = getUniqueBean(Werewolf.class);
        assertEquals(RequestScoped.class, werewolfBean.getScope());
    }

    @Test
    void processSynthethicBeanEventFired() {
        LifecycleObserver extension = beanManager.getExtension(LifecycleObserver.class);
        assertTrue(extension.isSkeletonPSBFired());
        assertTrue(extension.isVampirePSBFired());
        assertTrue(extension.isZombiePSBFired());
        assertTrue(extension.isGhostPSBFired());
    }

    private <T> void spawnMonster(Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = getUniqueBean(type, qualifiers);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        bean.create(creationalContext);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private <T> T getContextualReference(Class<T> beanType, Annotation... qualifiers) {
        Bean<T> bean = getUniqueBean(beanType, qualifiers);
        return beanType.cast(beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean)));
    }
}
