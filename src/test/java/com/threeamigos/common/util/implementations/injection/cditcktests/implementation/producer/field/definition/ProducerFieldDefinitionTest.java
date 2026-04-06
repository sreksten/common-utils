package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.producer.field.definition;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("serial")
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class ProducerFieldDefinitionTest {

    private static final Annotation TAME_LITERAL = new Tame.Literal();
    private static final Annotation PET_LITERAL = new Pet.Literal();
    private static final Annotation FOO_LITERAL = new Foo.Literal();
    private static final Annotation STATIC_LITERAL = new Static.Literal();

    @Test
    void testParameterizedReturnType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            FunnelWeaverSpiderConsumer spiderConsumer = getContextualReference(beanManager, FunnelWeaverSpiderConsumer.class);
            assertNotNull(spiderConsumer);
            assertNotNull(spiderConsumer.getInjectedSpider());
            assertEquals(FunnelWeaverSpiderProducer.getSpider(), spiderConsumer.getInjectedSpider());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanDeclaresMultipleProducerFields() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            assertEquals(1, getBeans(beanManager, Tarantula.class, TAME_LITERAL).size());
            assertEquals(OtherSpiderProducer.WOLF_SPIDER, getContextualReference(beanManager, WolfSpider.class, PET_LITERAL));
            assertEquals(1, getBeans(beanManager, BlackWidow.class, TAME_LITERAL).size());
            assertEquals(OtherSpiderProducer.BLACK_WIDOW, getContextualReference(beanManager, BlackWidow.class, TAME_LITERAL));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDefaultBindingType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> tarantulaBeans = getBeans(beanManager, Tarantula.class);
            assertEquals(2, tarantulaBeans.size());
            assertTrue(tarantulaBeans.iterator().next().getQualifiers().contains(Default.Literal.INSTANCE));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForClassReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> tarantulaBeans = getBeans(beanManager, Tarantula.class, PET_LITERAL);
            assertEquals(1, tarantulaBeans.size());
            Bean<?> tarantulaBean = tarantulaBeans.iterator().next();
            assertEquals(6, tarantulaBean.getTypes().size());
            assertTrue(tarantulaBean.getTypes().contains(Tarantula.class));
            assertTrue(tarantulaBean.getTypes().contains(DeadlySpider.class));
            assertTrue(tarantulaBean.getTypes().contains(Spider.class));
            assertTrue(tarantulaBean.getTypes().contains(Animal.class));
            assertTrue(tarantulaBean.getTypes().contains(DeadlyAnimal.class));
            assertTrue(tarantulaBean.getTypes().contains(Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForInterfaceReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> animalBeans = getBeans(beanManager, Animal.class, new AsAnimal.Literal());
            assertEquals(1, animalBeans.size());
            Bean<?> animalBean = animalBeans.iterator().next();
            assertEquals(2, animalBean.getTypes().size());
            assertTrue(animalBean.getTypes().contains(Animal.class));
            assertTrue(animalBean.getTypes().contains(Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForPrimitiveReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> beans = beanManager.getBeans("SpiderSize");
            assertEquals(1, beans.size());
            Bean<?> intBean = beans.iterator().next();
            assertEquals(2, intBean.getTypes().size());
            assertTrue(intBean.getTypes().contains(int.class));
            assertTrue(intBean.getTypes().contains(Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeForArrayTypeReturn() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> spiderBeans = getBeans(beanManager, Spider[].class);
            assertEquals(1, spiderBeans.size());
            Bean<?> spiderArrayBean = spiderBeans.iterator().next();
            assertEquals(2, spiderArrayBean.getTypes().size());
            assertTrue(spiderArrayBean.getTypes().contains(Spider[].class));
            assertTrue(spiderArrayBean.getTypes().contains(Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBindingType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> tarantulaBeans = getBeans(beanManager, Tarantula.class, TAME_LITERAL);
            assertEquals(1, tarantulaBeans.size());
            Bean<?> tarantulaBean = tarantulaBeans.iterator().next();
            assertEquals(3, tarantulaBean.getQualifiers().size());
            assertTrue(tarantulaBean.getQualifiers().contains(TAME_LITERAL));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testScopeType() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> tarantulaBeans = getBeans(beanManager, Tarantula.class, TAME_LITERAL, FOO_LITERAL);
            assertFalse(tarantulaBeans.isEmpty());
            Bean<?> tarantulaBean = tarantulaBeans.iterator().next();
            assertEquals(RequestScoped.class, tarantulaBean.getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNamedField() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> beans = beanManager.getBeans("blackWidow");
            assertEquals(1, beans.size());
            Bean<?> blackWidowBean = beans.iterator().next();
            assertEquals("blackWidow", blackWidowBean.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDefaultNamedByStereotype() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<?> staticTarantulaBean = getUniqueBean(beanManager, Tarantula.class, STATIC_LITERAL);
            assertEquals("produceTarantula", staticTarantulaBean.getName());
            assertTrue(annotationSetMatches(staticTarantulaBean.getQualifiers(), Any.Literal.INSTANCE, STATIC_LITERAL));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testDefaultNamed() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Bean<?> tarantulaBean = getUniqueBean(beanManager, Tarantula.class, PET_LITERAL);
            assertEquals("producedPetTarantula", tarantulaBean.getName());
            assertTrue(annotationSetMatches(tarantulaBean.getQualifiers(), Any.Literal.INSTANCE, PET_LITERAL,
                    NamedLiteral.of("producedPetTarantula")));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStereotype() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> tarantulaBeans = getBeans(beanManager, Tarantula.class, STATIC_LITERAL);
            assertFalse(tarantulaBeans.isEmpty());
            Bean<?> tarantulaBean = tarantulaBeans.iterator().next();
            assertEquals(RequestScoped.class, tarantulaBean.getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonStaticProducerFieldNotInherited() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Egg egg = getContextualReference(beanManager, Egg.class, FOO_LITERAL);
            assertFalse(egg.getMother() instanceof InfertileChicken);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonStaticProducerFieldNotIndirectlyInherited() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Egg egg = getContextualReference(beanManager, Egg.class, FOO_LITERAL);
            assertFalse(egg.getMother() instanceof LameInfertileChicken);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerFieldWithTypeVariable() {
        Syringe syringe = startContainer();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            List<Spider> spiders = getContextualReference(beanManager, new TypeLiteral<List<Spider>>() {
            });
            assertNotNull(spiders);
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe startContainer() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AsAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BlackWidow.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BlackWidowProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Chicken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DaddyLongLegs.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlyAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlySpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DefangedTarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Egg.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FunnelWeaver.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FunnelWeaverSpiderConsumer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FunnelWeaverSpiderProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(InfertileChicken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LadybirdSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LameInfertileChicken.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(OtherSpiderProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Pet.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderAsAnimalProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderListProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spidery.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Static.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(StaticTarantulaProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TameTarantulaProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TarantulaProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(WolfSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    private static Set<Bean<?>> getBeans(BeanManager beanManager, Class<?> type, Annotation... qualifiers) {
        return beanManager.getBeans(type, qualifiers);
    }

    private static Bean<?> getUniqueBean(BeanManager beanManager, Class<?> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        assertEquals(1, beans.size());
        return beans.iterator().next();
    }

    private static Bean<?> resolveBean(BeanManager beanManager, Type type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        return beanManager.resolve(beans);
    }

    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Bean<?> bean = resolveBean(beanManager, type, qualifiers);
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getContextualReference(BeanManager beanManager, TypeLiteral<T> typeLiteral,
                                                Annotation... qualifiers) {
        Type type = typeLiteral.getType();
        Bean<?> bean = resolveBean(beanManager, type, qualifiers);
        return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
    }

    private static boolean annotationSetMatches(Set<Annotation> actual, Annotation... expected) {
        return actual.size() == expected.length && actual.containsAll(Arrays.asList(expected));
    }
}
