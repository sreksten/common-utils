package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanManager;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.InjectionException;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.interceptor.InterceptorBinding;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeanManagerTest {

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                AfterBeanDiscoveryObserver.class,
                AnimalStereotype.class,
                Cow.class,
                CowBean.class,
                DerivedBean.class,
                Dog.class,
                DogHouse.class,
                DummyContext.class,
                DummyScoped.class,
                InheritedLiteral.class,
                InjectionPointDecorator.class,
                RetentionLiteral.class,
                SimpleBean.class,
                Snake.class,
                StereotypeLiteral.class,
                Tame.class,
                TargetLiteral.class,
                Terrier.class,
                Transactional.class,
                UnregisteredExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterBeanDiscoveryObserver.class.getName());
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
    void testValidateThrowsException() {
        DogHouse dogHouse = getContextualReference(DogHouse.class);
        InjectionPoint injectionPoint = new InjectionPointDecorator(dogHouse.getDog().getInjectedMetadata());
        assertThrows(InjectionException.class, () -> beanManager.validate(injectionPoint));
    }

    @Test
    void testGetMetaAnnotationsForStereotype() {
        Set<Annotation> stereotypeAnnotations = beanManager.getStereotypeDefinition(AnimalStereotype.class);
        assertEquals(5, stereotypeAnnotations.size());
        assertTrue(stereotypeAnnotations.contains(StereotypeLiteral.INSTANCE));
        assertTrue(stereotypeAnnotations.contains(RequestScoped.Literal.INSTANCE));
        assertTrue(stereotypeAnnotations.contains(InheritedLiteral.INSTANCE));
        assertTrue(stereotypeAnnotations.contains(new RetentionLiteral() {
            public RetentionPolicy value() {
                return RetentionPolicy.RUNTIME;
            }
        }));
        assertTrue(stereotypeAnnotations.contains(new TargetLiteral() {
            public ElementType[] value() {
                return new ElementType[]{TYPE, METHOD, FIELD};
            }
        }));
    }

    @Test
    void testGetMetaAnnotationsForInterceptorBindingType() {
        Set<Annotation> metaAnnotations = beanManager.getInterceptorBindingDefinition(Transactional.class);
        assertEquals(4, metaAnnotations.size());
        assertTrue(annotationSetMatches(metaAnnotations, Target.class, Retention.class, Documented.class, InterceptorBinding.class));
    }

    @Test
    void testDetermineScopeType() {
        assertTrue(beanManager.isNormalScope(RequestScoped.class));
        assertFalse(beanManager.isPassivatingScope(RequestScoped.class));
        assertTrue(beanManager.isNormalScope(SessionScoped.class));
        assertTrue(beanManager.isPassivatingScope(SessionScoped.class));
        assertTrue(beanManager.isNormalScope(DummyScoped.class));
        assertFalse(beanManager.isPassivatingScope(DummyScoped.class));
    }

    @Test
    void testGetELResolver() {
        assertNotNull(beanManager.getELResolver());
    }

    @Test
    void testObtainingAnnotatedType() {
        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(DerivedBean.class);
        assertTrue(annotatedType.isAnnotationPresent(Specializes.class));
        assertTrue(annotatedType.isAnnotationPresent(Tame.class));
        assertEquals(1, annotatedType.getFields().size());
        assertTrue(annotatedType.getMethods().isEmpty());
        assertEquals(3, annotatedType.getTypeClosure().size());
    }

    @Test
    void testObtainingInjectionTarget() {
        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(DerivedBean.class);
        assertNotNull(beanManager.getInjectionTargetFactory(annotatedType).createInjectionTarget(null));
    }

    @Test
    void testObtainingInjectionTargetWithDefinitionError() {
        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(Snake.class);
        assertThrows(IllegalArgumentException.class,
                () -> beanManager.getInjectionTargetFactory(annotatedType).createInjectionTarget(null));
    }

    @Test
    void testGetExtension() {
        AfterBeanDiscoveryObserver extension = beanManager.getExtension(AfterBeanDiscoveryObserver.class);
        assertNotNull(extension);
        assertTrue(extension.getAfterBeanDiscoveryObserved());

        Throwable thrown = assertThrows(Throwable.class, () -> beanManager.getExtension(UnregisteredExtension.class));
        assertTrue(isThrowablePresent(IllegalArgumentException.class, thrown));
    }

    @Test
    void testManagerBeanIsPassivationCapable() {
        assertTrue(isSerializable(beanManager.getClass()));
    }

    private static boolean annotationSetMatches(Set<Annotation> annotations,
                                                Class<? extends Annotation>... expectedTypes) {
        if (annotations.size() != expectedTypes.length) {
            return false;
        }
        Set<Class<? extends Annotation>> actualTypes = new HashSet<Class<? extends Annotation>>();
        for (Annotation annotation : annotations) {
            actualTypes.add(annotation.annotationType());
        }
        return actualTypes.containsAll(Arrays.asList(expectedTypes));
    }

    private static boolean isThrowablePresent(Class<? extends Throwable> throwableType, Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (throwableType.isAssignableFrom(current.getClass())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isSerializable(Class<?> clazz) {
        return clazz.isPrimitive() || Serializable.class.isAssignableFrom(clazz);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T getContextualReference(Class<T> beanType, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanType, qualifiers);
        Bean<T> bean = (Bean<T>) beanManager.resolve((Set) beans);
        return (T) beanManager.getReference(bean, beanType, beanManager.createCreationalContext(bean));
    }
}
