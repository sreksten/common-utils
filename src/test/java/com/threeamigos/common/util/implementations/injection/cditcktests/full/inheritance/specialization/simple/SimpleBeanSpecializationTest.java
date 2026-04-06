package com.threeamigos.common.util.implementations.injection.cditcktests.full.inheritance.specialization.simple;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleBeanSpecializationTest {

    private static final Annotation LANDOWNER_LITERAL = new Landowner.Literal();
    private static final Annotation LAZY_LITERAL = new Lazy.Literal();

    @Test
    void testIndirectSpecialization() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<Human>> humanBeans = getBeans(beanManager, Human.class);
            assertEquals(1, humanBeans.size());

            Set<Bean<Farmer>> farmerBeans = getBeans(beanManager, Farmer.class, LANDOWNER_LITERAL);
            assertEquals(1, farmerBeans.size());

            Bean<Farmer> lazyFarmerBean = farmerBeans.iterator().next();
            assertEquals(lazyFarmerBean.getBeanClass(), humanBeans.iterator().next().getBeanClass());

            Set<Type> lazyFarmerBeanTypes = lazyFarmerBean.getTypes();
            assertEquals(4, lazyFarmerBeanTypes.size());
            assertTrue(typeSetMatches(lazyFarmerBeanTypes, Object.class, Human.class, Farmer.class, LazyFarmer.class));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSpecializingBeanInjection() {
        Syringe syringe = newSyringe();
        try {
            Farmer farmer = getContextualReference(syringe.getBeanManager(), Farmer.class);
            assertEquals(LazyFarmer.class.getName(), farmer.getClassName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSpecializingBeanHasQualifiersOfSpecializedAndSpecializingBean() {
        Syringe syringe = newSyringe();
        try {
            Bean<LazyFarmer> lazyFarmerBean = getBeans(syringe.getBeanManager(), LazyFarmer.class, LAZY_LITERAL).iterator().next();
            Set<Annotation> lazyFarmerBeanQualifiers = lazyFarmerBean.getQualifiers();
            assertEquals(5, lazyFarmerBeanQualifiers.size());
            assertTrue(annotationSetMatches(
                    lazyFarmerBeanQualifiers,
                    Landowner.class,
                    Lazy.class,
                    Any.class,
                    Named.class,
                    Default.class
            ));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSpecializingBeanHasNameOfSpecializedBean() {
        Syringe syringe = newSyringe();
        try {
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> beans = beanManager.getBeans("farmer");
            assertEquals(1, beans.size());
            Bean<?> farmerBean = beans.iterator().next();
            assertEquals("farmer", farmerBean.getName());
            assertEquals(LazyFarmer.class, farmerBean.getBeanClass());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testProducerMethodOnSpecializedBeanNotCalled() {
        Syringe syringe = newSyringe();
        try {
            assertEquals(0, getBeans(syringe.getBeanManager(), Waste.class).size());
            assertThrows(UnsatisfiedResolutionException.class,
                    () -> getContextualReference(syringe.getBeanManager(), Waste.class));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                Animal.class,
                Building.class,
                Egg.class,
                Farmer.class,
                Human.class,
                Landowner.class,
                Lazy.class,
                LazyFarmer.class,
                Office.class,
                Waste.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Set<Bean<T>> getBeans(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        return (Set) beanManager.getBeans(type, qualifiers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(BeanManager beanManager, Class<T> type, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(type, qualifiers);
        if (beans == null || beans.isEmpty()) {
            throw new UnsatisfiedResolutionException(
                    "No bean matches required type " + type + " and required qualifiers " + Arrays.toString(qualifiers));
        }
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, type, beanManager.createCreationalContext(bean));
    }

    private static boolean typeSetMatches(Set<Type> types, Type... requiredTypes) {
        return requiredTypes.length == types.size() && types.containsAll(Arrays.asList(requiredTypes));
    }

    @SafeVarargs
    private static boolean annotationSetMatches(Set<? extends Annotation> annotations,
                                                Class<? extends Annotation>... requiredAnnotationTypes) {
        Set<Class<? extends Annotation>> annotationTypes = new HashSet<Class<? extends Annotation>>();
        for (Annotation annotation : annotations) {
            annotationTypes.add(annotation.annotationType());
        }
        return requiredAnnotationTypes.length == annotationTypes.size()
                && annotationTypes.containsAll(Arrays.asList(requiredAnnotationTypes));
    }
}
