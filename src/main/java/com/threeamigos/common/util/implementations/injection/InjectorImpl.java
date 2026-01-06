package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.interfaces.injection.ScopeHandler;
import org.jspecify.annotations.NonNull;

import javax.enterprise.inject.*;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The InjectorImpl class is responsible for injecting dependencies into classes, using the constructor annotated
 * with {@link Inject}.<br/> Only one constructor is allowed to be annotated with @Inject. If such a constructor
 * is not found, the Injector will try to instantiate the class using the no-args constructor. This is done to
 * instantiate dependencies needed by the class.<br/>
 * It uses the ClassResolver to find concrete implementations of abstract classes and interfaces.<br/>
 * The Unit Test for this class gives information about the expected behavior and edge cases.
 *
 * @author Stefano Reksten
 */
public class InjectorImpl implements Injector {

    private final Map<Class<? extends Annotation>, ScopeHandler> scopeRegistry = new HashMap<>();
    private final ClassResolver classResolver;
    private final String packageName;

    public InjectorImpl() {
        this.classResolver = new ClassResolver();
        this.packageName = "";
        registerDefaultScopes();
    }

    public InjectorImpl(final String packageName) {
        this.classResolver = new ClassResolver();
        this.packageName = packageName;
        registerDefaultScopes();
    }

    private void registerDefaultScopes() {
        // Standard Singleton scope implementation
        scopeRegistry.put(Singleton.class, new ScopeHandler() {
            private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(Class<T> clazz, Supplier<T> provider) {
                return (T) instances.computeIfAbsent(clazz, c -> provider.get());
            }
        });
    }

    @Override
    public void registerScope(Class<? extends Annotation> scopeAnnotation, ScopeHandler handler) {
        scopeRegistry.put(scopeAnnotation, handler);
    }

    @Override
    public void enableAlternative(Class<?> alternativeClass) {
        classResolver.enableAlternative(alternativeClass);
    }

    public <T> T inject(@NonNull Class<T> classToInject) throws Exception {
        return inject(classToInject, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T inject(@NonNull Class<T> classToInject, Annotation qualifier) {
        try {
            checkClassValidity(classToInject);
            Class<? extends T> resolvedClass = classResolver.resolveImplementation(classToInject, packageName, qualifier);

            // Find the scope annotation on the resolved class
            Class<? extends Annotation> scopeType = getScopeType(resolvedClass);

            if (scopeType != null && scopeRegistry.containsKey(scopeType)) {
                ScopeHandler handler = scopeRegistry.get(scopeType);
                // Use a helper method to handle the wildcard capture safely
                return handleScopedInjection(handler, resolvedClass);
            }

            return performInjection(resolvedClass);
        } catch (UnsatisfiedResolutionException | AmbiguousResolutionException e) {
            throw e;
        } catch (Exception e) {
            throw new InjectionException("Failed to inject " + classToInject.getName(), e);
        }
    }

    /**
     * Helper method to bridge the generic gap between Class<? extends T> and ScopeHandler
     */
    @SuppressWarnings("unchecked")
    private <T> T handleScopedInjection(ScopeHandler handler, Class<? extends T> clazz) {
        return (T) handler.get((Class<Object>) clazz, () -> performInjection(clazz));
    }

    /**
     * The actual instantiation logic (moved out of inject to be used by ScopeHandlers)
     */
    private <T> T performInjection(Class<? extends T> resolvedClass) {
        try {
            Constructor<? extends T> constructor = getConstructor(resolvedClass);
            Parameter[] parameters = constructor.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                if (javax.inject.Provider.class.isAssignableFrom(param.getType())) {
                    args[i] = createInstanceWrapper(param);
                } else {
                    checkClassValidity(param.getType());
                    Annotation paramQualifier = getQualifier(param);
                    args[i] = inject(param.getType(), paramQualifier);
                }
            }
            return buildInstance(constructor, args);
        } catch (Exception e) {
            throw new InjectionException("Instantiation failed for " + resolvedClass.getName(), e);
        }
    }

    private Class<? extends Annotation> getScopeType(Class<?> clazz) {
        return Arrays.stream(clazz.getAnnotations())
                .map(Annotation::annotationType)
                .filter(at -> at.isAnnotationPresent(Scope.class) || at.equals(Singleton.class))
                .findFirst()
                .orElse(null);
    }
    /**
     * Checks that the class is valid for injection.
     * @param clazz the class to check
     */
    private void checkClassValidity(Class<?> clazz) {
        if (clazz.isEnum()) {
            throw new IllegalArgumentException("Cannot inject an enum");
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Cannot inject a primitive");
        }
        if (clazz.isSynthetic()) {
            throw new IllegalArgumentException("Cannot inject a synthetic class");
        }
        // Let's keep this simple
        if (clazz.isLocalClass()) {
            throw new IllegalArgumentException("Cannot inject a local class");
        }
        if (clazz.isAnonymousClass()) {
            throw new IllegalArgumentException("Cannot inject an anonymous class");
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new IllegalArgumentException("Cannot inject a non-static inner class");
        }
    }

    private boolean isSingleton(Class<?> clazz) {
        // Check for the standard @Singleton or any custom annotation marked with @Scope
        return clazz.isAnnotationPresent(Singleton.class) ||
                // In this implementation, for standalone applications, every scoped bean is managed as a Singleton.
                Arrays.stream(clazz.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(Scope.class));
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> getConstructor(@NonNull Class<T> clazz) throws NoSuchMethodException {
        List<Constructor<T>> constructors = Arrays.stream((Constructor<T>[])clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (constructors.size() > 1) {
            throw new IllegalStateException("More than one constructor annotated with @Inject");
        } else if (constructors.size() == 1) {
            return constructors.get(0);
        }
        // No @Inject constructor found, let's try to find a no-argument constructor
        constructors = Arrays.stream((Constructor<T>[])clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .collect(Collectors.toList());
        if (constructors.isEmpty()) {
            throw new NoSuchMethodException("No empty constructor or a constructor annotated with @Inject in class " + clazz.getName());
        }
        return constructors.get(0);
    }

    private Annotation getQualifier(Parameter param) {
        return Arrays.stream(param.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .findFirst()
                .orElse(null);
    }

    private Instance<?> createInstanceWrapper(java.lang.reflect.Parameter param) {
        ParameterizedType type = (ParameterizedType) param.getParameterizedType();
        Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
        boolean isAny = param.isAnnotationPresent(Any.class);
        Annotation qualifier = getQualifier(param);

        return createInstance(genericType, qualifier, isAny);
    }

    private <T> Instance<T> createInstance(Class<T> type, Annotation qualifier, boolean isAny) {
        return new Instance<T>() {
            @Override
            public T get() {
                try {
                    return inject(type, qualifier);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to inject " + type.getName(), e);
                }
            }

            @Override
            public Instance<T> select(Annotation... annotations) {
                Annotation newQualifier = annotations.length > 0 ? annotations[0] : qualifier;
                return createInstance(type, newQualifier, isAny);
            }

            @Override
            public <U extends T> Instance<U> select(Class<U> subtype, Annotation... annotations) {
                Annotation newQualifier = annotations.length > 0 ? annotations[0] : qualifier;
                return createInstance(subtype, newQualifier, isAny);
            }

            @Override
            public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... annotations) {
                throw new UnsupportedOperationException("TypeLiteral selection not supported");
            }

            @Override
            public boolean isUnsatisfied() {
                try {
                    return classResolver.resolveImplementations(type, packageName).isEmpty();
                } catch (Exception e) {
                    return true;
                }
            }

            @Override
            public boolean isAmbiguous() {
                try {
                    return classResolver.resolveImplementations(type, packageName).size() > 1;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void destroy(T instance) {
                // No-op
            }

            @Override
            public @NonNull Iterator<T> iterator() {
                try {
                    Collection<? extends Class<? extends T>> classes = isAny
                            ? classResolver.resolveImplementations(type, packageName)
                            : Collections.singletonList(classResolver.resolveImplementation(type, packageName, qualifier));

                    List<T> instances = new ArrayList<>();
                    for (Class<? extends T> clazz : classes) {
                        instances.add(inject(clazz));
                    }
                    return instances.iterator();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to resolve implementations", e);
                }
            }
        };
    }

    <T> T buildInstance(Constructor<? extends T> constructor, Object... args) throws Exception {
        if (!Modifier.isPublic(constructor.getModifiers())) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance(args);
    }

}
