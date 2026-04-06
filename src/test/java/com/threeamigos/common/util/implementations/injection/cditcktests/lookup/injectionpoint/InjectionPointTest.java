package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.injectionpoint;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjectionPointTest {

    @Test
    void testGetBean() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());

            Set<Bean<?>> resolvedBeans = syringe.getBeanManager().getBeans(FieldInjectionPointBean.class);
            assertEquals(1, resolvedBeans.size());
            assertEquals(resolvedBeans.iterator().next(), beanWithInjectionPoint.getInjectedMetadata().getBean());
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testNullInjectionPointInjectedIntoNonInjectedObject() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            Foo foo = getContextualReference(syringe, Foo.class);
            assertTrue(foo.isEveryInjectionPointNull(),
                    "A non-null value of InjectionPoint injected into a dependent object that is not being injected.");

            getContextualReference(syringe, Baz.class).ping();
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testGetType() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertEquals(BeanWithInjectionPointMetadata.class, beanWithInjectionPoint.getInjectedMetadata().getType());
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testGetBindingTypes() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            Set<Annotation> bindingTypes = beanWithInjectionPoint.getInjectedMetadata().getQualifiers();
            assertEquals(1, bindingTypes.size());
            assertTrue(Default.class.isAssignableFrom(bindingTypes.iterator().next().annotationType()));
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testGetMemberField() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertInstanceOf(Field.class, beanWithInjectionPoint.getInjectedMetadata().getMember());
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testGetMemberMethod() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            MethodInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, MethodInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertInstanceOf(Method.class, beanWithInjectionPoint.getInjectedMetadata().getMember());
            assertEquals(BeanWithInjectionPointMetadata.class, beanWithInjectionPoint.getInjectedMetadata().getType());
            assertTrue(beanWithInjectionPoint.getInjectedMetadata().getQualifiers().contains(Default.Literal.INSTANCE));
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testGetMemberConstructor() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            ConstructorInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, ConstructorInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertInstanceOf(Constructor.class, beanWithInjectionPoint.getInjectedMetadata().getMember());
            assertEquals(BeanWithInjectionPointMetadata.class, beanWithInjectionPoint.getInjectedMetadata().getType());
            assertTrue(beanWithInjectionPoint.getInjectedMetadata().getQualifiers().contains(Default.Literal.INSTANCE));
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testGetAnnotatedField() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertInstanceOf(AnnotatedField.class, beanWithInjectionPoint.getInjectedMetadata().getAnnotated());
            assertTrue(beanWithInjectionPoint.getInjectedMetadata().getAnnotated().isAnnotationPresent(Animal.class));
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testGetAnnotatedParameter() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            MethodInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, MethodInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertInstanceOf(AnnotatedParameter.class, beanWithInjectionPoint.getInjectedMetadata().getAnnotated());
            assertAnnotationTypes(beanWithInjectionPoint.getInjectedMetadata().getQualifiers(), Default.class);
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testDependentScope() {
        Syringe syringe = newSyringe();
        try {
            Set<Bean<?>> beans = syringe.getBeanManager().getBeans(InjectionPoint.class);
            assertEquals(1, beans.size());
            assertEquals(Dependent.class, beans.iterator().next().getScope());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testApiTypeInjectionPoint() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertTrue(InjectionPoint.class.isAssignableFrom(beanWithInjectionPoint.getInjectedMetadata().getClass()));
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testCurrentBinding() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean beanWithInjectedBean = getContextualReference(syringe, FieldInjectionPointBean.class);
            BeanWithInjectionPointMetadata beanWithInjectionPoint = beanWithInjectedBean.getInjectedBean();
            assertNotNull(beanWithInjectionPoint.getInjectedMetadata());
            assertTrue(beanWithInjectionPoint.getInjectedMetadata().getQualifiers().contains(Default.Literal.INSTANCE));
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    @Test
    void testIsTransient() {
        Syringe syringe = newSyringe();
        BeanManagerImpl beanManager = beanManager(syringe);
        beanManager.getContextManager().activateRequest();
        try {
            FieldInjectionPointBean bean1 = getContextualReference(syringe, FieldInjectionPointBean.class);
            TransientFieldInjectionPointBean bean2 = getContextualReference(syringe, TransientFieldInjectionPointBean.class);
            InjectionPoint ip1 = bean1.getInjectedBean().getInjectedMetadata();
            InjectionPoint ip2 = bean2.getInjectedBean().getInjectedMetadata();
            assertTrue(!ip1.isTransient());
            assertTrue(ip2.isTransient());
        } finally {
            beanManager.getContextManager().deactivateRequest();
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Baz.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BazProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(BeanWithInjectionPointMetadata.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ConstructorInjectionPointBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FieldInjectionPointBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Foo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MethodInjectionPointBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TransientFieldInjectionPointBean.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    private <T> T getContextualReference(Syringe syringe, Class<T> type) {
        Bean<T> bean = getBean(syringe, type);
        CreationalContext<T> creationalContext = syringe.getBeanManager().createCreationalContext(bean);
        return type.cast(syringe.getBeanManager().getReference(bean, type, creationalContext));
    }

    private BeanManagerImpl beanManager(Syringe syringe) {
        return (BeanManagerImpl) syringe.getBeanManager();
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> getBean(Syringe syringe, Class<T> type) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(type);
        return (Bean<T>) syringe.getBeanManager().resolve(beans);
    }

    private void assertAnnotationTypes(Set<Annotation> annotations, Class<? extends Annotation>... expectedTypes) {
        assertEquals(expectedTypes.length, annotations.size());
        for (Class<? extends Annotation> expectedType : expectedTypes) {
            boolean found = false;
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(expectedType)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Missing annotation type: " + expectedType.getName());
        }
    }
}
