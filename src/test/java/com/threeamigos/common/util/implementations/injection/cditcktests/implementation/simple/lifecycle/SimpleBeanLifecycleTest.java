package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.simple.lifecycle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class SimpleBeanLifecycleTest {

    @Test
    void testInjectionOfParametersIntoBeanConstructor() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                assertEquals(1, getBeans(beanManager, FishPond.class).size());
                FishPond fishPond = getContextualReference(beanManager, FishPond.class);
                assertNotNull(fishPond.goldfish);
                assertTrue(fishPond.goldfish instanceof Goldfish);
                assertNotNull(fishPond.goose);
            }
        });
    }

    @Test
    void testQualifierTypeAnnotatedConstructor() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                Duck.constructedCorrectly = false;
                getContextualReference(syringe.getBeanManager(), Duck.class);
                assertTrue(Duck.constructedCorrectly);
            }
        });
    }

    @Test
    void testCreateReturnsSameBeanPushed() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                Bean<ShoeFactory> bean = resolveBean(beanManager, ShoeFactory.class);
                InspectableCreationalContext<ShoeFactory> creationalContext =
                        new InspectableCreationalContext<ShoeFactory>(beanManager.createCreationalContext(bean));

                Context dependentContext = beanManager.getContext(Dependent.class);
                ShoeFactory instance = dependentContext.get(bean, creationalContext);

                if (creationalContext.isPushCalled()) {
                    assertSame(instance, creationalContext.getLastBeanPushed());
                }
            }
        });
    }

    @Test
    void testBeanCreateInjectsDependenciesAndInvokesInitializerToInstantiateInstance() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                Bean<FishPond> bean = resolveBean(beanManager, FishPond.class);
                CreationalContext<FishPond> creationalContext = beanManager.createCreationalContext(bean);
                FishPond fishPond = bean.create(creationalContext);
                assertNotNull(fishPond);
                assertNotNull(fishPond.goldfish);
                assertTrue(fishPond.goldfish instanceof Goldfish);
                assertNotNull(fishPond.goose);
                assertNotNull(fishPond.salmon);
                assertTrue(fishPond.postConstructCalled);
            }
        });
    }

    @Test
    void testManagedBean() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                assertEquals(1, getBeans(beanManager, RedSnapper.class).size());
                RedSnapper redSnapper = getContextualReference(beanManager, RedSnapper.class);
                redSnapper.ping();
                assertTrue(redSnapper.isTouched());
            }
        });
    }

    @Test
    void testCreateInjectsFieldsDeclaredInJava() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                assertEquals(1, getBeans(beanManager, TunaFarm.class).size());
                TunaFarm tunaFarm = getContextualReference(beanManager, TunaFarm.class);
                assertNotNull(tunaFarm.tuna);
                assertEquals("Ophir", tunaFarm.tuna.getName());
                assertNotNull(tunaFarm.qualifiedTuna);
                assertEquals("qualifiedTuna", tunaFarm.qualifiedTuna.getName());
            }
        });
    }

    @Test
    void testContextCreatesNewInstanceForInjection() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                Context requestContext = beanManager.getContext(RequestScoped.class);
                Bean<Tuna> tunaBean = resolveBean(beanManager, Tuna.class);
                assertNull(requestContext.get(tunaBean));
                TunaFarm tunaFarm = getContextualReference(beanManager, TunaFarm.class);
                assertNotNull(tunaFarm.tuna);
            }
        });
    }

    @Test
    void testPostConstructPreDestroy() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                assertEquals(1, getBeans(beanManager, Farm.class).size());
                Bean<Farm> farmBean = resolveBean(beanManager, Farm.class);
                CreationalContext<Farm> creationalContext = beanManager.createCreationalContext(farmBean);
                Farm farm = farmBean.create(creationalContext);
                assertNotNull(farm.founded);
                assertEquals(20, farm.initialStaff);
                assertNull(farm.closed);
                farmBean.destroy(farm, creationalContext);
                assertNotNull(farm.closed);
                assertEquals(0, farm.farmOffice.noOfStaff);
            }
        });
    }

    @Test
    void testContextualDestroyDisposesWhenNecessary() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                Bean<Goose> gooseBean = resolveBean(beanManager, Goose.class);
                CreationalContext<Goose> gooseCc = beanManager.createCreationalContext(gooseBean);
                Goose goose = gooseBean.create(gooseCc);
                assertFalse(isProxy(goose));
                assertFalse(EggProducer.isEggDisposed());
                assertFalse(Egg.isEggDestroyed());
                gooseBean.destroy(goose, gooseCc);
                assertTrue(EggProducer.isEggDisposed());
                assertFalse(Egg.isEggDestroyed());
            }
        });
    }

    @Test
    void testContextualDestroyCatchesException() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                Bean<Cod> codBean = resolveBean(beanManager, Cod.class);
                CreationalContext<Cod> creationalContext = beanManager.createCreationalContext(codBean);
                Cod codInstance = codBean.create(creationalContext);
                codInstance.ping();
                codBean.destroy(codInstance, creationalContext);
                assertTrue(Cod.isExpcetionThrown());
            }
        });
    }

    @Test
    void testDependentsDestroyedAfterPreDestroy() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                Bean<FishPond> pondBean = resolveBean(beanManager, FishPond.class);
                CreationalContext<FishPond> creationalContext = beanManager.createCreationalContext(pondBean);
                FishPond fishPond = pondBean.create(creationalContext);
                pondBean.destroy(fishPond, creationalContext);
                assertTrue(Salmon.isBeanDestroyed());
            }
        });
    }

    @Test
    void testSubClassInheritsPostConstructOnSuperclass() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                OrderProcessor.postConstructCalled = false;
                assertEquals(1, getBeans(beanManager, CdOrderProcessor.class).size());
                getContextualReference(beanManager, CdOrderProcessor.class).order();
                assertTrue(OrderProcessor.postConstructCalled);

                Instance<Object> instance = beanManager.createInstance();
                OrderProcessor.postConstructCalled = false;
                instance.select(CdOrderProcessor.class).get().order();
                assertTrue(OrderProcessor.postConstructCalled);
            }
        });
    }

    @Test
    void testIndirectSubClassInheritsPostConstructOnSuperclass() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                OrderProcessor.postConstructCalled = false;
                assertEquals(1, getBeans(beanManager, IndirectOrderProcessor.class).size());
                getContextualReference(beanManager, IndirectOrderProcessor.class).order();
                assertTrue(OrderProcessor.postConstructCalled);
            }
        });
    }

    @Test
    void testSubClassInheritsPreDestroyOnSuperclass() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                OrderProcessor.preDestroyCalled = false;
                assertEquals(1, getBeans(beanManager, CdOrderProcessor.class).size());

                DependentInstance<CdOrderProcessor> bean = newDependentInstance(beanManager, CdOrderProcessor.class);
                assertNotNull(bean.get());
                bean.destroy();
                assertTrue(OrderProcessor.preDestroyCalled);
            }
        });
    }

    @Test
    void testIndirectSubClassInheritsPreDestroyOnSuperclass() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                OrderProcessor.preDestroyCalled = false;
                assertEquals(1, getBeans(beanManager, IndirectOrderProcessor.class).size());
                DependentInstance<IndirectOrderProcessor> bean = newDependentInstance(beanManager, IndirectOrderProcessor.class);
                assertNotNull(bean.get());
                bean.destroy();
                assertTrue(OrderProcessor.preDestroyCalled);
            }
        });
    }

    @Test
    void testSubClassDoesNotInheritPostConstructOnSuperclassBlockedByIntermediateClass() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                assertEquals(1, getBeans(beanManager, NovelOrderProcessor.class).size());
                OrderProcessor.postConstructCalled = false;
                getContextualReference(beanManager, NovelOrderProcessor.class).order();
                assertFalse(OrderProcessor.postConstructCalled);
            }
        });
    }

    @Test
    void testSubClassDoesNotInheritPreDestroyConstructOnSuperclassBlockedByIntermediateClass() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                OrderProcessor.preDestroyCalled = false;
                assertEquals(1, getBeans(beanManager, NovelOrderProcessor.class).size());
                DependentInstance<NovelOrderProcessor> bean = newDependentInstance(beanManager, NovelOrderProcessor.class);
                assertNotNull(bean.get());
                bean.destroy();
                assertFalse(OrderProcessor.preDestroyCalled);
            }
        });
    }

    @Test
    void testCreationExceptionWrapsCheckedExceptionThrownFromCreate() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                assertEquals(1, getBeans(beanManager, Lorry_Broken.class).size());
                assertThrows(CreationException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        getContextualReference(beanManager, Lorry_Broken.class);
                    }
                });
            }
        });
    }

    @Test
    void testUncheckedExceptionThrownFromCreateNotWrapped() {
        withSyringe(new TestLogic() {
            @Override
            public void run(Syringe syringe) {
                BeanManager beanManager = syringe.getBeanManager();
                assertEquals(1, getBeans(beanManager, Van_Broken.class).size());
                assertThrows(FooException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        getContextualReference(beanManager, Van_Broken.class);
                    }
                });
            }
        });
    }

    private void withSyringe(TestLogic logic) {
        Syringe syringe = newSyringe();
        boolean activatedRequest = syringe.activateRequestContextIfNeeded();
        try {
            logic.run(syringe);
        } finally {
            if (activatedRequest) {
                syringe.deactivateRequestContextIfActive();
            }
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BookOrderProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(CdOrderProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cod.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Duck.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Egg.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(EggProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Farm.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FarmOffice.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FishPond.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FishStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FooException.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Goldfish.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Goose.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IndirectOrderProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(IntermediateOrderProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Lorry_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NovelOrderProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(OrderProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(RedSnapper.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(RequestScopedAnimalStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Salmon.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ShoeFactory.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Synchronous.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tuna.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TunaFarm.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TunaProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Van_Broken.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return (Set) beanManager.getBeans(type, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = (Set) beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    private static <T> DependentInstance<T> newDependentInstance(BeanManager beanManager, Class<T> type,
                                                                 Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        T instance = type.cast(beanManager.getReference(bean, type, creationalContext));
        return new DependentInstance<T>(bean, creationalContext, instance);
    }

    private static boolean isProxy(Object instance) {
        if (instance == null) {
            return false;
        }
        String name = instance.getClass().getName();
        return name.contains("$$")
                || name.contains("$Proxy")
                || name.contains("$ByteBuddy$")
                || name.startsWith("com.sun.proxy.$Proxy");
    }

    private interface TestLogic {
        void run(Syringe syringe);
    }

    private static class DependentInstance<T> {

        private final Bean<T> bean;
        private final CreationalContext<T> creationalContext;
        private final T instance;

        private DependentInstance(Bean<T> bean, CreationalContext<T> creationalContext, T instance) {
            this.bean = bean;
            this.creationalContext = creationalContext;
            this.instance = instance;
        }

        private T get() {
            return instance;
        }

        private void destroy() {
            bean.destroy(instance, creationalContext);
        }
    }

    private static class InspectableCreationalContext<T> implements CreationalContext<T> {

        private final CreationalContext<T> delegate;
        private boolean pushCalled;
        private T lastBeanPushed;

        private InspectableCreationalContext(CreationalContext<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void push(T incompleteInstance) {
            pushCalled = true;
            lastBeanPushed = incompleteInstance;
            delegate.push(incompleteInstance);
        }

        @Override
        public void release() {
            delegate.release();
        }

        private boolean isPushCalled() {
            return pushCalled;
        }

        private T getLastBeanPushed() {
            return lastBeanPushed;
        }
    }
}
