package com.threeamigos.common.util.implementations.injection.cditcktests.interceptors.definition;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("serial")
@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InterceptorDefinitionTest {

    private static final Transactional.TransactionalLiteral TRANSACTIONAL_LITERAL = new Transactional.TransactionalLiteral("") {
    };

    private static final AnnotationLiteral<Secure> SECURE_LITERAL = new Secure.Literal();

    private static final AnnotationLiteral<MissileBinding> MISSILE_LITERAL = new MissileBinding.Literal();

    private static final AnnotationLiteral<Logged> LOGGED_LITERAL = new Logged.Literal();

    private static final AnnotationLiteral<Atomic> ATOMIC_LITERAL = new Atomic.Literal();

    @Test
    void testInterceptorsImplementInterceptorInterface() {
        Syringe syringe = newSyringe();
        try {
            boolean interfaceFound = false;
            for (Type type : getInterfacesImplemented(getTransactionalInterceptor(syringe).getClass())) {
                if (type instanceof ParameterizedType
                        && Interceptor.class.equals(((ParameterizedType) type).getRawType())) {
                    interfaceFound = true;
                    break;
                }
            }
            assertTrue(interfaceFound);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorBindingTypes() {
        Syringe syringe = newSyringe();
        try {
            Interceptor<?> interceptorBean = getTransactionalInterceptor(syringe);
            assertEquals(1, interceptorBean.getInterceptorBindings().size());
            assertTrue(interceptorBean.getInterceptorBindings().contains(TRANSACTIONAL_LITERAL));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptionType() {
        Syringe syringe = newSyringe();
        try {
            Interceptor<?> interceptorBean = getTransactionalInterceptor(syringe);
            assertTrue(interceptorBean.intercepts(InterceptionType.AROUND_INVOKE));
            assertFalse(interceptorBean.intercepts(InterceptionType.POST_ACTIVATE));
            assertFalse(interceptorBean.intercepts(InterceptionType.POST_CONSTRUCT));
            assertFalse(interceptorBean.intercepts(InterceptionType.PRE_DESTROY));
            assertFalse(interceptorBean.intercepts(InterceptionType.PRE_PASSIVATE));
            assertFalse(interceptorBean.intercepts(InterceptionType.AROUND_TIMEOUT));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInstanceOfInterceptorForEveryEnabledInterceptor() {
        Syringe syringe = newSyringe();
        try {
            List<AnnotationLiteral<?>> annotationLiterals = Arrays.<AnnotationLiteral<?>>asList(
                    TRANSACTIONAL_LITERAL,
                    SECURE_LITERAL,
                    MISSILE_LITERAL,
                    LOGGED_LITERAL
            );

            List<Class<?>> interceptorClasses = new ArrayList<Class<?>>(Arrays.<Class<?>>asList(
                    AtomicInterceptor.class,
                    MissileInterceptor.class,
                    SecureInterceptor.class,
                    TransactionalInterceptor.class,
                    NetworkLogger.class,
                    FileLogger.class,
                    NotEnabledAtomicInterceptor.class
            ));

            for (AnnotationLiteral<?> annotationLiteral : annotationLiterals) {
                List<Interceptor<?>> interceptors = syringe.getBeanManager().resolveInterceptors(
                        InterceptionType.AROUND_INVOKE,
                        annotationLiteral
                );
                for (Interceptor<?> interceptor : interceptors) {
                    interceptorClasses.remove(interceptor.getBeanClass());
                }
            }

            List<Interceptor<?>> interceptors = syringe.getBeanManager().resolveInterceptors(
                    InterceptionType.AROUND_INVOKE,
                    ATOMIC_LITERAL,
                    MISSILE_LITERAL
            );
            for (Interceptor<?> interceptor : interceptors) {
                interceptorClasses.remove(interceptor.getBeanClass());
            }

            assertEquals(1, interceptorClasses.size());
            assertEquals(NotEnabledAtomicInterceptor.class, interceptorClasses.get(0));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testResolveInterceptorsReturnsOrderedList() {
        Syringe syringe = newSyringe();
        try {
            List<Interceptor<?>> interceptors = syringe.getBeanManager().resolveInterceptors(
                    InterceptionType.AROUND_INVOKE,
                    TRANSACTIONAL_LITERAL,
                    SECURE_LITERAL
            );

            assertEquals(2, interceptors.size());
            assertEquals(1, interceptors.get(0).getInterceptorBindings().size());
            assertTrue(interceptors.get(0).getInterceptorBindings().contains(SECURE_LITERAL));
            assertEquals(1, interceptors.get(1).getInterceptorBindings().size());
            assertTrue(interceptors.get(1).getInterceptorBindings().contains(TRANSACTIONAL_LITERAL));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testSameBindingTypesToResolveInterceptorsFails() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() {
                    syringe.getBeanManager().resolveInterceptors(
                            InterceptionType.AROUND_INVOKE,
                            new Transactional.TransactionalLiteral("a"),
                            new Transactional.TransactionalLiteral("b")
                    );
                }
            });
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNoBindingTypesToResolveInterceptorsFails() {
        Syringe syringe = newSyringe();
        try {
            assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() {
                    syringe.getBeanManager().resolveInterceptors(InterceptionType.AROUND_INVOKE);
                }
            });
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testNonBindingTypeToResolveInterceptorsFails() {
        Syringe syringe = newSyringe();
        try {
            final Annotation nonBinding = new NonBindingType.Literal();
            assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
                @Override
                public void execute() {
                    syringe.getBeanManager().resolveInterceptors(InterceptionType.AROUND_INVOKE, nonBinding);
                }
            });
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorBindingAnnotation() {
        Syringe syringe = newSyringe();
        try {
            List<Interceptor<?>> interceptors = getLoggedInterceptors(syringe);
            assertTrue(interceptors.size() > 1);

            Interceptor<?> interceptorBean = interceptors.iterator().next();
            assertEquals(1, interceptorBean.getInterceptorBindings().size());
            assertTrue(interceptorBean.getInterceptorBindings().contains(LOGGED_LITERAL));

            Target target = interceptorBean.getInterceptorBindings().iterator().next()
                    .annotationType()
                    .getAnnotation(Target.class);
            List<ElementType> elements = Arrays.asList(target.value());
            assertTrue(elements.contains(ElementType.TYPE));
            assertTrue(elements.contains(ElementType.METHOD));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testStereotypeInterceptorBindings() {
        Syringe syringe = newSyringe();
        try {
            FileLogger.intercepted = false;
            NetworkLogger.intercepted = false;

            SecureTransaction secureTransaction = getContextualReference(syringe, SecureTransaction.class);
            secureTransaction.transact();

            assertTrue(FileLogger.intercepted);
            assertTrue(NetworkLogger.intercepted);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testInterceptorBindingsCanDeclareOtherInterceptorBindings() {
        Syringe syringe = newSyringe();
        try {
            AtomicInterceptor.intercepted = false;
            MissileInterceptor.intercepted = false;

            AtomicFoo foo = getContextualReference(syringe, AtomicFoo.class);
            foo.doAction();

            assertTrue(AtomicInterceptor.intercepted);
            assertTrue(MissileInterceptor.intercepted);
        } finally {
            syringe.shutdown();
        }
    }

    private Interceptor<?> getTransactionalInterceptor(Syringe syringe) {
        return syringe.getBeanManager()
                .resolveInterceptors(InterceptionType.AROUND_INVOKE, TRANSACTIONAL_LITERAL)
                .iterator()
                .next();
    }

    private List<Interceptor<?>> getLoggedInterceptors(Syringe syringe) {
        return syringe.getBeanManager().resolveInterceptors(InterceptionType.AROUND_INVOKE, new Logged.Literal());
    }

    private Set<Type> getInterfacesImplemented(Class<?> clazz) {
        Set<Type> interfaces = new HashSet<Type>();
        collectHierarchyTypes(clazz, interfaces);
        return interfaces;
    }

    private void collectHierarchyTypes(Type type, Set<Type> result) {
        if (type == null || !result.add(type)) {
            return;
        }
        Class<?> rawType = rawType(type);
        if (rawType == null) {
            return;
        }
        for (Type iface : rawType.getGenericInterfaces()) {
            collectHierarchyTypes(iface, result);
        }
        Type superType = rawType.getGenericSuperclass();
        if (superType != null && !Object.class.equals(superType)) {
            collectHierarchyTypes(superType, result);
        }
    }

    private Class<?> rawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw instanceof Class<?>) {
                return (Class<?>) raw;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getContextualReference(Syringe syringe, Class<T> type) {
        Set<jakarta.enterprise.inject.spi.Bean<T>> beans = (Set) syringe.getBeanManager().getBeans(type);
        jakarta.enterprise.inject.spi.Bean<T> bean = (jakarta.enterprise.inject.spi.Bean<T>) syringe.getBeanManager().resolve((Set) beans);
        return type.cast(syringe.getBeanManager().getReference(bean, type, syringe.getBeanManager().createCreationalContext(bean)));
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(AccountTransaction.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Atomic.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AtomicFoo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AtomicInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FileLogger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Loggable.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Logged.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MissileBinding.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(MissileInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NetworkLogger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NonBindingType.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(NotEnabledAtomicInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Secure.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SecureInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SecureTransaction.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ShoppingCart.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Transactional.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(TransactionalInterceptor.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }
}
