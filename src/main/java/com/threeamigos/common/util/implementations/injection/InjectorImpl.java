package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.interfaces.injection.ScopeHandler;
import org.jspecify.annotations.NonNull;

import javax.enterprise.inject.*;
import javax.enterprise.util.TypeLiteral;
import javax.inject.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The InjectorImpl class is responsible for injecting dependencies into classes, using the constructor annotated
 * with {@link Inject}.<br/> Only one constructor is allowed to be annotated with @Inject. If such a constructor
 * is not found, the Injector will try to instantiate the class using the no-args constructor. This is done to
 * instantiate dependencies needed by the class.<br/>
 * The Unit Test for this class gives information about the expected behavior and edge cases.
 *
 * @author Stefano Reksten
 */
public class InjectorImpl implements Injector {

    /**
     * Internal registry for scope handlers. By default, it contains only the SingletonScopeHandler.
     */
    private final Map<Class<? extends Annotation>, ScopeHandler> scopeRegistry = new HashMap<>();

    /**
     * The class resolver used in this injector to find concrete implementations of abstract classes and interfaces.
     */
    private final ClassResolver classResolver;

    /**
     * Package name is used to restrict classes search to a particular package. If you don't want to
     * scan the whole classpath, you can use your package name here. Beware - all your interfaces and
     * implementations should reside in that package or subpackages!
     */
    private final String packageName;

    public InjectorImpl() {
        this.classResolver = new ClassResolver();
        this.packageName = "";
        registerDefaultScope();
    }

    public InjectorImpl(final String packageName) {
        this.classResolver = new ClassResolver();
        this.packageName = packageName;
        registerDefaultScope();
    }

    InjectorImpl(final ClassResolver classResolver, final String packageName) {
        this.classResolver = classResolver;
        this.packageName = packageName;
        registerDefaultScope();
    }

    private void registerDefaultScope() {
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

    @Override
    public <T> T inject(@NonNull Class<T> classToInject) {
        return inject(classToInject, new Stack<Class<?>>(), null);
    }

    private <T> T inject(@NonNull Class<T> classToInject, Stack<Class<?>> stack, Annotation qualifier) {
        if (stack.contains(classToInject)) {
            throw new InjectionException("Circular dependency detected for class " + classToInject.getName() + ": " + stack);
        }
        stack.push(classToInject);
        try {
            checkClassValidity(classToInject);
            Class<? extends T> resolvedClass = classResolver.resolveImplementation(classToInject, packageName, qualifier);

            // Find the scope annotation on the resolved class
            Class<? extends Annotation> scopeType = getScopeType(resolvedClass);

            if (scopeType != null && scopeRegistry.containsKey(scopeType)) {
                ScopeHandler handler = scopeRegistry.get(scopeType);
                // We use a helper method to handle the wildcard capture safely
                T t = handleScopedInjection(handler, resolvedClass, stack);
                stack.pop();
                return t;
            }

            T t = performInjection(resolvedClass, stack);
            stack.pop();
            return t;
        } catch (UnsatisfiedResolutionException | AmbiguousResolutionException e) {
            throw e;
        } catch (Exception e) {
            throw new InjectionException("Failed to inject " + classToInject.getName(), e);
        }
    }

    /**
     * NOTE: this method supports one annotation only at a time.
     * @param clazz class to check
     */
    private Class<? extends Annotation> getScopeType(Class<?> clazz) {
        return Arrays.stream(clazz.getAnnotations())
                .map(Annotation::annotationType)
                .filter(at -> at.isAnnotationPresent(Scope.class) || at.equals(Singleton.class))
                .findFirst()
                .orElse(null);
    }

    /**
     * Helper method to bridge the generic gap between Class<? extends T> and ScopeHandler
     */
    @SuppressWarnings("unchecked")
    private <T> T handleScopedInjection(ScopeHandler handler, Class<? extends T> clazz, Stack<Class<?>> stack) {
        return (T) handler.get((Class<Object>) clazz, () -> performInjection(clazz, stack));
    }

    /**
     * The actual instantiation logic
     */
    private <T> T performInjection(Class<? extends T> resolvedClass, Stack<Class<?>> stack) {
        try {
            Constructor<? extends T> constructor = getConstructor(resolvedClass);
            Object[] args = resolveParameters(constructor.getParameters(), stack);
            T t = buildInstance(constructor, args);
            injectFields(t, stack);
            injectMethods(t, stack);
            return t;
        } catch (Exception e) {
            throw new InjectionException("Instantiation failed for " + resolvedClass.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> getConstructor(@NonNull Class<T> clazz) throws NoSuchMethodException {
        List<Constructor<T>> constructors = Arrays.stream((Constructor<T>[])clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (constructors.size() > 1) {
            throw new IllegalStateException("More than one constructor annotated with @Inject in class " + clazz.getName());
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

    private Object[] resolveParameters(Parameter[] parameters, Stack<Class<?>> stack) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (javax.inject.Provider.class.isAssignableFrom(param.getType())) {
                args[i] = createInstanceWrapper(param);
            } else {
                checkClassValidity(param.getType());
                Annotation paramQualifier = getQualifier(param);
                args[i] = inject(param.getType(), stack, paramQualifier);
            }
        }
        return args;
    }

    <T> T buildInstance(Constructor<? extends T> constructor, Object... args) throws Exception {
        if (!Modifier.isPublic(constructor.getModifiers())) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance(args);
    }

    private <T> void injectFields(T t, Stack<Class<?>> stack) throws Exception {
        Field[] fields = t.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new IllegalStateException("Cannot inject into final field " + field.getName() + " of class " +
                            field.getClass().getName());
                }
                field.setAccessible(true);
                if (Provider.class.isAssignableFrom(field.getType())) {
                    field.set(t, createInstanceWrapper(field));
                } else {
                    checkClassValidity(field.getType());
                    Annotation fieldQualifier = getQualifier(field);
                    field.set(t, inject(field.getType(), stack, fieldQualifier));
                }
            }
        }
    }

    private<T> void injectMethods(T t, Stack<Class<?>> stack) throws Exception {
        Method[] methods = t.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Inject.class)) {
                method.setAccessible(true);
                Object[] params = resolveParameters(method.getParameters(), stack);
                method.invoke(t, params);
            }
        }
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

    private Annotation getQualifier(Parameter param) {
        return Arrays.stream(param.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .findFirst()
                .orElse(null);
    }

    private Annotation getQualifier(Field field) {
        return Arrays.stream(field.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .findFirst()
                .orElse(null);
    }

    private Instance<?> createInstanceWrapper(Field field) {
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
        boolean isAny = field.isAnnotationPresent(Any.class);
        Annotation qualifier = getQualifier(field);

        return createInstance(genericType, qualifier, isAny);
    }

    private Instance<?> createInstanceWrapper(Parameter param) {
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
                    return inject(type, new Stack<Class<?>>(), qualifier);
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
                    return true; // treating an Exception as unsatisfied
                }
            }

            @Override
            public boolean isAmbiguous() {
                try {
                    return classResolver.resolveImplementations(type, packageName).size() > 1;
                } catch (Exception e) {
                    return false; // If we can't resolve the class, it's not ambiguous (it's unsatisfied)
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

}
