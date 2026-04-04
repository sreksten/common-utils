package com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.DomesticationKit;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.Farm;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.Fox;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.FoxFarm;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.FoxHole;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.FoxRun;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.Horse;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.HorseInStableEvent;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.HorseStable;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.OtherSpiderProducer;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.Pet;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.SensitiveFox;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.SpiderProducer;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.Stable;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.Tame;
import com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test.Tarantula;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependentContextTest {

    private static final String FIXTURE_PACKAGE =
            "com.threeamigos.common.util.implementations.injection.cditcktests.context.dependent.dependentcontexttest.test";

    private static final Annotation TAME_LITERAL = new Tame.Literal();
    private static final Annotation PET_LITERAL = new Pet.Literal();

    @Test
    void testInstanceNotSharedBetweenInjectionPoints() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<FoxRun> foxRunBean = resolveBean(beanManager, FoxRun.class);
                CreationalContext<FoxRun> creationalContext = beanManager.createCreationalContext(foxRunBean);
                FoxRun foxRun = foxRunBean.create(creationalContext);
                assertNotEquals(foxRun.fox, foxRun.anotherFox);
                assertNotEquals(foxRun.fox, foxRun.petFox);
                assertNotEquals(foxRun.anotherFox, foxRun.petFox);
                foxRunBean.destroy(foxRun, creationalContext);
            }
        });
    }

    @Test
    void testDependentBeanIsDependentObjectOfBeanInjectedInto() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                FoxFarm foxFarm = getContextualReference(beanManager, FoxFarm.class);
                FoxHole foxHole = getContextualReference(beanManager, FoxHole.class);

                assertNotEquals(foxFarm.fox, foxHole.fox);
                assertNotEquals(foxFarm.fox, foxFarm.constructorFox);
                assertNotEquals(foxFarm.constructorFox, foxHole.initializerFox);
                assertNotEquals(foxHole.fox, foxHole.initializerFox);
            }
        });
    }

    @Test
    void testInstanceUsedForProducerMethodNotShared() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                SpiderProducer.reset();
                getContextualReference(beanManager, Tarantula.class, PET_LITERAL);
                Integer firstInstanceHash = SpiderProducer.getInstanceUsedForProducerHashcode();

                SpiderProducer.reset();
                getContextualReference(beanManager, Tarantula.class, PET_LITERAL);
                Integer secondInstanceHash = SpiderProducer.getInstanceUsedForProducerHashcode();

                assertNotNull(firstInstanceHash);
                assertNotNull(secondInstanceHash);
                assertNotEquals(firstInstanceHash, secondInstanceHash);
            }
        });
    }

    @Test
    void testInstanceUsedForProducerFieldNotShared() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Tarantula firstInstance = getContextualReference(beanManager, Tarantula.class, TAME_LITERAL);
                Tarantula secondInstance = getContextualReference(beanManager, Tarantula.class, TAME_LITERAL);

                assertNotNull(firstInstance.getProducerInstanceHashcode());
                assertNotNull(secondInstance.getProducerInstanceHashcode());
                assertNotEquals(firstInstance.getProducerInstanceHashcode(), secondInstance.getProducerInstanceHashcode());
            }
        });
    }

    @Test
    void testInstanceUsedForDisposalMethodNotShared() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Integer firstFoxHash = getContextualReference(beanManager, Fox.class).hashCode();

                SpiderProducer.reset();
                SpiderProducer spiderProducer = getContextualReference(beanManager, SpiderProducer.class);
                Bean<Tarantula> tarantulaBean = resolveBean(beanManager, Tarantula.class, PET_LITERAL);
                CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(tarantulaBean);
                Tarantula tarantula = tarantulaBean.create(creationalContext);
                assertNotNull(tarantula);

                tarantulaBean.destroy(tarantula, creationalContext);
                Integer secondFoxHash = SpiderProducer.getFoxUsedForDisposalHashcode();

                assertNotNull(SpiderProducer.getInstanceUsedForDisposalHashcode());
                assertNotEquals(SpiderProducer.getInstanceUsedForDisposalHashcode(), spiderProducer.hashCode());

                CreationalContext<Tarantula> nextCreationalContext = beanManager.createCreationalContext(tarantulaBean);
                Tarantula nextTarantula = tarantulaBean.create(nextCreationalContext);
                assertNotNull(nextTarantula);

                tarantulaBean.destroy(nextTarantula, nextCreationalContext);
                Integer thirdFoxHash = SpiderProducer.getFoxUsedForDisposalHashcode();

                assertNotEquals(firstFoxHash, secondFoxHash);
                assertNotEquals(firstFoxHash, thirdFoxHash);
                assertNotEquals(secondFoxHash, thirdFoxHash);

                SpiderProducer.reset();
            }
        });
    }

    @Test
    void testInstanceUsedForObserverMethodNotShared() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                HorseStable.reset();
                HorseStable firstStableInstance = getContextualReference(beanManager, HorseStable.class);

                @SuppressWarnings("unchecked")
                Event<HorseInStableEvent> event = (Event<HorseInStableEvent>) beanManager.getEvent().select(HorseInStableEvent.class);
                event.fire(new HorseInStableEvent());
                Integer firstFoxHash = HorseStable.getFoxUsedForObservedEventHashcode();
                Integer firstObserverHash = HorseStable.getInstanceThatObservedEventHashcode();

                event.fire(new HorseInStableEvent());
                Integer secondFoxHash = HorseStable.getFoxUsedForObservedEventHashcode();
                Integer secondObserverHash = HorseStable.getInstanceThatObservedEventHashcode();

                assertNotNull(firstObserverHash);
                assertNotNull(secondObserverHash);
                assertNotNull(firstFoxHash);
                assertNotNull(secondFoxHash);
                assertNotEquals(firstStableInstance.hashCode(), firstObserverHash);
                assertNotEquals(firstStableInstance.hashCode(), secondObserverHash);
                assertNotEquals(firstObserverHash, secondObserverHash);
                assertNotEquals(firstFoxHash, secondFoxHash);
            }
        });
    }

    @Test
    void testContextGetWithCreationalContextReturnsNewInstance() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Fox> foxBean = resolveBean(beanManager, Fox.class);
                Context context = beanManager.getContext(Dependent.class);
                assertNotNull(context.get(foxBean, beanManager.createCreationalContext(foxBean)));
                assertTrue(context.get(foxBean, beanManager.createCreationalContext(foxBean)) instanceof Fox);
            }
        });
    }

    @Test
    void testContextGetWithCreateFalseReturnsNull() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Fox> foxBean = resolveBean(beanManager, Fox.class);
                Context context = beanManager.getContext(Dependent.class);
                assertNull(context.get(foxBean, null));
            }
        });
    }

    @Test
    void testContextScopeType() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertTrue(beanManager.getContext(Dependent.class).getScope().equals(Dependent.class));
            }
        });
    }

    @Test
    void testContextIsActive() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                assertTrue(beanManager.getContext(Dependent.class).isActive());
            }
        });
    }

    @Test
    void testContextIsActiveWhenInvokingProducerMethod() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                SpiderProducer.reset();
                Tarantula tarantula = getContextualReference(beanManager, Tarantula.class, PET_LITERAL);
                assertNotNull(tarantula);
                assertTrue(SpiderProducer.isDependentContextActive());
                SpiderProducer.reset();
            }
        });
    }

    @Test
    void testContextIsActiveWhenInvokingProducerField() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Tarantula.reset();
                getContextualReference(beanManager, Tarantula.class, TAME_LITERAL);
                assertTrue(Tarantula.isDependentContextActive());
                SpiderProducer.reset();
            }
        });
    }

    @Test
    void testContextIsActiveWhenInvokingDisposalMethod() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Tarantula> tarantulaBean = resolveBean(beanManager, Tarantula.class, PET_LITERAL);
                CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(tarantulaBean);
                Tarantula tarantula = tarantulaBean.create(creationalContext);
                assertNotNull(tarantula);
                SpiderProducer.reset();
                tarantulaBean.destroy(tarantula, creationalContext);
                assertTrue(SpiderProducer.isDependentContextActive());
                SpiderProducer.reset();
            }
        });
    }

    @Test
    void testContextIsActiveWhenCreatingObserverMethodInstance() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                @SuppressWarnings("unchecked")
                Event<HorseInStableEvent> event = (Event<HorseInStableEvent>) beanManager.getEvent().select(HorseInStableEvent.class);
                event.fire(new HorseInStableEvent());
                assertTrue(HorseStable.isDependentContextActive());
            }
        });
    }

    @Test
    void testContextIsActiveDuringBeanCreation() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                SensitiveFox fox = getContextualReference(beanManager, SensitiveFox.class);
                assertNotNull(fox);
                assertTrue(fox.isDependentContextActiveDuringCreate());
            }
        });
    }

    @Test
    void testContextIsActiveDuringInjection() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<FoxRun> foxRunBean = resolveBean(beanManager, FoxRun.class);
                FoxRun foxRun = foxRunBean.create(beanManager.createCreationalContext(foxRunBean));
                assertNotNull(foxRun.fox);
            }
        });
    }

    @Test
    void testDestroyingSimpleParentDestroysDependents() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Farm> farmBean = resolveBean(beanManager, Farm.class);
                CreationalContext<Farm> creationalContext = beanManager.createCreationalContext(farmBean);
                Farm farm = farmBean.create(creationalContext);
                farm.open();
                Stable.destroyed = false;
                Horse.destroyed = false;
                farmBean.destroy(farm, creationalContext);
                assertTrue(Stable.destroyed);
                assertTrue(Horse.destroyed);
            }
        });
    }

    @Test
    void testCallingCreationalContextReleaseDestroysDependents() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Farm> farmBean = resolveBean(beanManager, Farm.class);

                CreationalContext<Farm> creationalContext = beanManager.createCreationalContext(farmBean);
                Farm farm = farmBean.create(creationalContext);
                farm.open();
                Stable.destroyed = false;
                Horse.destroyed = false;
                creationalContext.release();
                assertTrue(Stable.destroyed);
                assertTrue(Horse.destroyed);

                creationalContext = beanManager.createCreationalContext(farmBean);
                farm = (Farm) beanManager.getReference(farmBean, Farm.class, creationalContext);
                farm.open();
                Stable.destroyed = false;
                Horse.destroyed = false;
                creationalContext.release();
                assertTrue(Stable.destroyed);
                assertTrue(Horse.destroyed);
            }
        });
    }

    @Test
    void testDestroyingManagedParentDestroysDependentsOfSameBean() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Fox.reset();
                Bean<FoxRun> bean = resolveBean(beanManager, FoxRun.class);
                CreationalContext<FoxRun> creationalContext = beanManager.createCreationalContext(bean);
                FoxRun instance = bean.create(creationalContext);
                assertNotEquals(instance.fox, instance.anotherFox);
                bean.destroy(instance, creationalContext);
                assertTrue(Fox.isDestroyed());
                assertTrue(Fox.getDestroyCount() == 2);
            }
        });
    }

    @Test
    void testDependentsDestroyedWhenProducerMethodCompletes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                SpiderProducer.reset();
                Tarantula.reset();
                DomesticationKit.reset();

                Bean<Tarantula> tarantulaBean = resolveBean(beanManager, Tarantula.class, PET_LITERAL);
                CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(tarantulaBean);
                Tarantula tarantula = (Tarantula) beanManager.getReference(tarantulaBean, Tarantula.class, creationalContext);
                tarantula.ping();
                assertTrue(SpiderProducer.isDestroyed());

                tarantulaBean.destroy(tarantula, creationalContext);
                assertTrue(DomesticationKit.isDestroyed());
                SpiderProducer.reset();
            }
        });
    }

    @Test
    void testDependentsDestroyedWhenProducerFieldCompletes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                OtherSpiderProducer.setDestroyed(false);
                Tarantula spiderInstance = getContextualReference(beanManager, Tarantula.class, TAME_LITERAL);
                assertNotNull(spiderInstance);
                assertTrue(OtherSpiderProducer.isDestroyed());
            }
        });
    }

    @Test
    void testDependentsDestroyedWhenDisposerMethodCompletes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                Bean<Tarantula> tarantulaBean = resolveBean(beanManager, Tarantula.class, PET_LITERAL);
                CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(tarantulaBean);
                Tarantula tarantula = tarantulaBean.create(creationalContext);
                assertNotNull(tarantula);

                SpiderProducer.reset();
                Fox.reset();

                tarantulaBean.destroy(tarantula, creationalContext);
                assertTrue(SpiderProducer.isDestroyed());
                assertNotNull(SpiderProducer.getFoxUsedForDisposalHashcode());
                assertTrue(Fox.isDestroyed());

                SpiderProducer.reset();
                Fox.reset();
            }
        });
    }

    @Test
    void testDependentsDestroyedWhenObserverMethodEvaluationCompletes() {
        runInContainer(new BeanManagerConsumer() {
            @Override
            public void accept(BeanManager beanManager) {
                HorseStable.reset();
                Fox.reset();
                @SuppressWarnings("unchecked")
                Event<HorseInStableEvent> event = (Event<HorseInStableEvent>) beanManager.getEvent().select(HorseInStableEvent.class);
                event.fire(new HorseInStableEvent());
                assertNotNull(HorseStable.getInstanceThatObservedEventHashcode());
                assertTrue(HorseStable.isDestroyed());
                assertTrue(Fox.isDestroyed());
            }
        });
    }

    private static void runInContainer(BeanManagerConsumer assertions) {
        Syringe syringe = new Syringe(FIXTURE_PACKAGE);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            assertions.accept(syringe.getBeanManager());
        } finally {
            syringe.shutdown();
        }
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private interface BeanManagerConsumer {
        void accept(BeanManager beanManager);
    }
}
