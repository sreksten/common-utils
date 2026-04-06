package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.typesafe.resolution;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("serial")
class ResolutionByTypeTest {

    private static final TypeLiteral<FlightlessBird<Australian>> AUSTRALIAN_FLIGHTLESS_BIRD =
            new TypeLiteral<FlightlessBird<Australian>>() {
            };
    private static final TypeLiteral<FlightlessBird<European>> EUROPEAN_FLIGHTLESS_BIRD =
            new TypeLiteral<FlightlessBird<European>>() {
            };
    private static final TypeLiteral<Cat<European>> EUROPEAN_CAT = new TypeLiteral<Cat<European>>() {
    };
    private static final TypeLiteral<Cat<African>> AFRICAN_CAT = new TypeLiteral<Cat<African>>() {
    };
    private static final Annotation TAME = new Tame.Literal();
    private static final Annotation WILD = new Wild.Literal();
    private static final Annotation NUMBER = new Number.Literal();

    @Test
    void testDefaultBindingTypeAssumed() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Tuna>> possibleTargets = getBeans(syringe, Tuna.class);
            assertEquals(1, possibleTargets.size());
            assertTrue(possibleTargets.iterator().next().getTypes().contains(Tuna.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testAllQualifiersSpecifiedForResolutionMustAppearOnBean() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Animal>> animalBeans = getBeans(syringe, Animal.class, new ChunkyLiteral(), new Whitefish.Literal());
            assertEquals(1, animalBeans.size());
            assertTrue(animalBeans.iterator().next().getTypes().contains(Cod.class));

            Set<Bean<ScottishFish>> scottishFishBeans = getBeans(syringe, ScottishFish.class, new Whitefish.Literal());
            assertEquals(2, scottishFishBeans.size());

            for (Bean<ScottishFish> bean : scottishFishBeans) {
                assertTrue(bean.getTypes().contains(Cod.class) || bean.getTypes().contains(Sole.class));
            }
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveByTypeWithTypeParameter() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Farmer<ScottishFish>>> beans = getBeans(syringe, new TypeLiteral<Farmer<ScottishFish>>() {
            });
            assertEquals(1, beans.size());
            assertTrue(beans.iterator().next().getTypes().contains(ScottishFishFarmer.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveByTypeWithArray() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, Spider[].class).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveByTypeWithPrimitives() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(2, getBeans(syringe, Double.class, NUMBER).size());
            assertEquals(2, getBeans(syringe, double.class, NUMBER).size());

            Double min = getContextualReference(syringe, Double.class, new Min.Literal());
            double max = getContextualReference(syringe, double.class, new Max.Literal());

            assertEquals(Double.valueOf(NumberProducer.min), min);
            assertEquals(NumberProducer.max, Double.valueOf(max));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveByTypeWithNonBindingMembers() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<Animal>> beans = getBeans(syringe, Animal.class, new ExpensiveLiteral() {
                public int cost() {
                    return 60;
                }

                public boolean veryExpensive() {
                    return true;
                }
            }, new Whitefish.Literal());
            assertEquals(2, beans.size());

            Set<Type> classes = new HashSet<Type>();
            for (Bean<Animal> bean : beans) {
                classes.addAll(bean.getTypes());
            }
            assertTrue(classes.contains(Halibut.class));
            assertTrue(classes.contains(RoundWhitefish.class));
            assertFalse(classes.contains(Sole.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanTypesOnManagedBean() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, Canary.class).size());
            Bean<Canary> bean = getUniqueBean(syringe, Canary.class);
            assertTrue(getBeans(syringe, Bird.class).isEmpty());
            assertTrue(typeSetMatches(bean.getTypes(), Canary.class, Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGenericBeanTypesOnManagedBean() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, AUSTRALIAN_FLIGHTLESS_BIRD).size());
            assertTrue(getBeans(syringe, Emu.class).isEmpty());
            assertTrue(getBeans(syringe, EUROPEAN_FLIGHTLESS_BIRD).isEmpty());

            Bean<FlightlessBird<Australian>> bean = getUniqueBean(syringe, AUSTRALIAN_FLIGHTLESS_BIRD);
            assertTrue(typeSetMatches(bean.getTypes(), AUSTRALIAN_FLIGHTLESS_BIRD.getType(), Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanTypesOnProducerMethod() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, Parrot.class).size());
            assertTrue(getBeans(syringe, Bird.class).isEmpty());

            Bean<Parrot> bean = getUniqueBean(syringe, Parrot.class);
            assertTrue(typeSetMatches(bean.getTypes(), Parrot.class, Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGenericBeanTypesOnProducerField() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, EUROPEAN_CAT, TAME).size());
            assertTrue(getBeans(syringe, DomesticCat.class, TAME).isEmpty());

            Bean<Cat<European>> bean = getUniqueBean(syringe, EUROPEAN_CAT, TAME);
            assertTrue(typeSetMatches(bean.getTypes(), EUROPEAN_CAT.getType(), Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGenericBeanTypesOnProducerMethod() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, AFRICAN_CAT, WILD).size());
            assertTrue(getBeans(syringe, Lion.class, WILD).isEmpty());

            Bean<Cat<African>> bean = getUniqueBean(syringe, AFRICAN_CAT, WILD);
            assertTrue(typeSetMatches(bean.getTypes(), AFRICAN_CAT.getType(), Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testBeanTypesOnProducerField() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(1, getBeans(syringe, Dove.class).size());
            assertTrue(getBeans(syringe, Bird.class).isEmpty());

            Bean<Dove> bean = getUniqueBean(syringe, Dove.class);
            assertTrue(typeSetMatches(bean.getTypes(), Dove.class, Object.class));
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();

        syringe.addDiscoveredClass(African.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnimalFarmer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Australian.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bird.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Canary.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cat.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Chunky.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Cod.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DaddyLongLegs.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlyAnimal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DeadlySpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(DomesticCat.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Dove.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Emu.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(European.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Expensive.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Farmer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FlightlessBird.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Halibut.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(LadybirdSpider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Lion.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Max.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Min.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Number.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NumberProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Parrot.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PetShop.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Plaice.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(RoundWhitefish.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ScottishFish.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ScottishFishFarmer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Sole.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Spider.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SpiderProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tame.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tarantula.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tuna.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Whitefish.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Wild.class, BeanArchiveMode.EXPLICIT);

        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(Syringe syringe, Class<T> type, Annotation... qualifiers) {
        return (Set) syringe.getBeanManager().getBeans(type, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(Syringe syringe, TypeLiteral<T> type, Annotation... qualifiers) {
        return (Set) syringe.getBeanManager().getBeans(type.getType(), qualifiers);
    }

    private static <T> Bean<T> getUniqueBean(Syringe syringe, Class<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = getBeans(syringe, type, qualifiers);
        assertEquals(1, beans.size());
        return beans.iterator().next();
    }

    private static <T> Bean<T> getUniqueBean(Syringe syringe, TypeLiteral<T> type, Annotation... qualifiers) {
        Set<Bean<T>> beans = getBeans(syringe, type, qualifiers);
        assertEquals(1, beans.size());
        return beans.iterator().next();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(Syringe syringe, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(type, qualifiers);
        Bean<T> bean = (Bean<T>) syringe.getBeanManager().resolve((Set) beans);
        return (T) syringe.getBeanManager().getReference(bean, type,
                syringe.getBeanManager().createCreationalContext(bean));
    }

    private static boolean typeSetMatches(Set<Type> actualTypes, Type... expectedTypes) {
        Set<Type> expected = new HashSet<Type>();
        Collections.addAll(expected, expectedTypes);
        return actualTypes.equals(expected);
    }
}
