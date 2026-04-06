package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.producer.method.definition;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("serial")
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class ProducerMethodDefinitionTest {

    private static final Annotation TAME_LITERAL = new Tame.Literal();
    private static final Annotation DEADLIEST_LITERAL = new Deadliest.Literal();
    private static final Annotation NUMBER_LITERAL = new Number.Literal();
    private static final Annotation YUMMY_LITERAL = new Yummy.Literal();

    @Test
    void testStaticMethod() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, String.class, TAME_LITERAL).size());
            assertEquals(BeanWithStaticProducerMethod.getString(), getContextualReference(beanManager, String.class, TAME_LITERAL));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerOnNonBean() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertTrue(getBeans(beanManager, Cherry.class).isEmpty());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStaticDisposerMethod() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, String.class, TAME_LITERAL).size());
            DependentInstance<String> stringBean = newDependentInstance(beanManager, String.class, TAME_LITERAL);
            assertTrue("Pete".equals(stringBean.get()));
            stringBean.destroy();
            assertTrue(BeanWithStaticProducerMethod.stringDestroyed);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStaticDisposerMethodWithNonStaticProducer() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, String.class, NUMBER_LITERAL).size());
            DependentInstance<String> stringBean = newDependentInstance(beanManager, String.class, NUMBER_LITERAL);
            assertTrue("number".equals(stringBean.get()));
            stringBean.destroy();
            assertTrue(BeanWithStaticProducerMethod.numberDestroyed);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonStaticDisposerMethodWithStaticProducer() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, String.class, YUMMY_LITERAL).size());
            DependentInstance<String> stringBean = newDependentInstance(beanManager, String.class, YUMMY_LITERAL);
            assertTrue("yummy".equals(stringBean.get()));
            stringBean.destroy();
            assertTrue(BeanWithStaticProducerMethod.yummyDestroyed);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testParameterizedReturnType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, new TypeLiteral<FunnelWeaver<Spider>>() {
            }).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDefaultBindingType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> beans = beanManager.getBeans(Tarantula.class);
            assertEquals(1, beans.size());
            assertEquals(2, beans.iterator().next().getQualifiers().size());
            assertTrue(beans.iterator().next().getQualifiers().contains(Default.Literal.INSTANCE));
            assertTrue(beans.iterator().next().getQualifiers().contains(Any.Literal.INSTANCE));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForClassReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, Tarantula.class).size());
            Bean<Tarantula> tarantula = getUniqueBean(beanManager, Tarantula.class);

            assertEquals(6, tarantula.getTypes().size());
            assertTrue(tarantula.getTypes().contains(Tarantula.class));
            assertTrue(tarantula.getTypes().contains(DeadlySpider.class));
            assertTrue(tarantula.getTypes().contains(Spider.class));
            assertTrue(tarantula.getTypes().contains(Animal.class));
            assertTrue(tarantula.getTypes().contains(DeadlyAnimal.class));
            assertTrue(tarantula.getTypes().contains(Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForInterfaceReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, Bite.class).size());
            Bean<Bite> bite = getUniqueBean(beanManager, Bite.class);
            assertEquals(2, bite.getTypes().size());
            assertTrue(bite.getTypes().contains(Bite.class));
            assertTrue(bite.getTypes().contains(Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForPrimitiveReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<Integer>> beans = getBeans(beanManager, Integer.class, NUMBER_LITERAL);
            assertEquals(1, beans.size());
            Bean<Integer> bean = beans.iterator().next();
            assertTypeSetMatches(bean.getTypes(), Object.class, int.class);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForArrayTypeReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, Spider[].class).size());
            Bean<Spider[]> spiders = getUniqueBean(beanManager, Spider[].class);
            assertEquals(2, spiders.getTypes().size());
            assertTrue(spiders.getTypes().contains(Spider[].class));
            assertTrue(spiders.getTypes().contains(Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBindingType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, Tarantula.class, TAME_LITERAL).size());
            Bean<Tarantula> tarantula = getUniqueBean(beanManager, Tarantula.class, TAME_LITERAL);
            assertEquals(2, tarantula.getQualifiers().size());
            assertTrue(tarantula.getQualifiers().contains(TAME_LITERAL));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testScopeType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, DaddyLongLegs.class, TAME_LITERAL).size());
            Bean<DaddyLongLegs> daddyLongLegs = getUniqueBean(beanManager, DaddyLongLegs.class, TAME_LITERAL);
            assertEquals(RequestScoped.class, daddyLongLegs.getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNamedMethod() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, BlackWidow.class, TAME_LITERAL).size());
            Bean<BlackWidow> blackWidowSpider = getUniqueBean(beanManager, BlackWidow.class, TAME_LITERAL);
            assertEquals("blackWidow", blackWidowSpider.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDefaultNamedMethod() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            String name = "produceDaddyLongLegs";
            Bean<DaddyLongLegs> daddyLongLegs = getUniqueBean(beanManager, DaddyLongLegs.class, TAME_LITERAL);
            assertEquals(name, daddyLongLegs.getName());
            assertTrue(annotationSetMatches(daddyLongLegs.getQualifiers(), Any.Literal.INSTANCE, TAME_LITERAL,
                    NamedLiteral.of(name)));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStereotypeSpecifiesScope() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, WolfSpider.class, TAME_LITERAL).size());
            Bean<WolfSpider> wolfSpider = getUniqueBean(beanManager, WolfSpider.class, TAME_LITERAL);
            assertEquals(RequestScoped.class, wolfSpider.getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonStaticProducerMethodNotInherited() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, Apple.class, YUMMY_LITERAL).size());
            assertEquals(AppleTree.class, getContextualReference(beanManager, Apple.class, YUMMY_LITERAL).getTree().getClass());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBindingTypesAppliedToProducerMethodParameters() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<Tarantula> tarantula = getUniqueBean(beanManager, Tarantula.class, DEADLIEST_LITERAL);
            CreationalContext<Tarantula> creationalContext = beanManager.createCreationalContext(tarantula);
            Tarantula instance = tarantula.create(creationalContext);
            assertEquals(1, instance.getDeathsCaused());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDependentProducerReturnsNullValue() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertNull(getContextualReference(beanManager, Acorn.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonDependentProducerReturnsNullValue() {
        Syringe syringe = startContainer();
        try {
            final BeanManager beanManager = syringe.getBeanManager();
            boolean activatedRequest = syringe.activateRequestContextIfNeeded();
            try {
                assertThrows(IllegalProductException.class, new org.junit.jupiter.api.function.Executable() {
                    @Override
                    public void execute() {
                        getContextualReference(beanManager, Pollen.class, new Yummy.Literal()).ping();
                    }
                });
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
    void testTypeVariableReturnType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, new TypeLiteral<List<Spider>>() {
            }).size());
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Acorn.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnimalStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Apple.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AppleTree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BeanWithStaticProducerMethod.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bite.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BlackWidow.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cherry.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DaddyLongLegs.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Deadliest.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlyAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlySpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DefangedTarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FunnelWeaver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GeneralListProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GrannySmithAppleTree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(GreatGrannySmithAppleTree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LadybirdSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NonBeanWithStaticProducerMethod.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Number.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(OakTree.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Pollen.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WolfSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Yummy.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return (Set) beanManager.getBeans(type, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, TypeLiteral<T> typeLiteral, Annotation... qualifiers) {
        return (Set) beanManager.getBeans(typeLiteral.getType(), qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> resolveBean(BeanManager beanManager, Type type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }

    private static <T> Bean<T> getUniqueBean(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = getBeans(beanManager, type, qualifiers);
        assertEquals(1, beans.size());
        return beans.iterator().next();
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getContextualReference(BeanManager beanManager, TypeLiteral<T> typeLiteral, Annotation... qualifiers) {
        Type type = typeLiteral.getType();
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
    }

    private static <T> DependentInstance<T> newDependentInstance(BeanManager beanManager, Class<T> type,
            Annotation... qualifiers) {
        Bean<T> bean = resolveBean(beanManager, type, qualifiers);
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        T instance = bean.create(creationalContext);
        return new DependentInstance<T>(bean, creationalContext, instance);
    }

    private static boolean annotationSetMatches(Set<Annotation> actual, Annotation... expected) {
        return actual.size() == expected.length && actual.containsAll(Arrays.asList(expected));
    }

    private static void assertTypeSetMatches(Set<Type> actualTypes, Type... expectedTypes) {
        assertEquals(expectedTypes.length, actualTypes.size());
        for (Type expectedType : expectedTypes) {
            assertTrue(actualTypes.contains(expectedType));
        }
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
}
