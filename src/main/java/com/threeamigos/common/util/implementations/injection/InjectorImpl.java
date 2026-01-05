package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.annotations.injection.Alternative;
import com.threeamigos.common.util.annotations.injection.Any;
import com.threeamigos.common.util.annotations.injection.Inject;
import com.threeamigos.common.util.annotations.injection.Singleton;
import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.interfaces.injection.Instance;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.*;
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

    private final Map<Class<?>, Object> singletonCache = new HashMap<>();
    private final ClassResolver classResolver;
    private final String packageName;

    public InjectorImpl() {
        this.classResolver = new ClassResolver();
        this.packageName = "";
    }

    public InjectorImpl(final String packageName) {
        this.classResolver = new ClassResolver();
        this.packageName = packageName;
    }

    public <T> T inject(@NonNull Class<T> classToInject) throws Exception {
        return inject(classToInject, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T inject(@NonNull Class<T> classToInject, String identifier) throws Exception {
        checkClassValidity(classToInject);

        Class<? extends T> resolvedClass = classResolver.resolveImplementation(classToInject, packageName, identifier);

        boolean singleton = isSingleton(resolvedClass);
        if (singleton && singletonCache.containsKey(resolvedClass)) {
            return (T) singletonCache.get(resolvedClass);
        }

        Constructor<? extends T> constructor = getConstructor(resolvedClass);

        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (Instance.class.isAssignableFrom(param.getType())) {
                args[i] = createInstanceWrapper(param);
            } else {
                checkClassValidity(param.getType());
                Alternative alternative = param.getAnnotation(Alternative.class);
                args[i] = inject(param.getType(), alternative != null ? alternative.value() : null);
            }
        }

        T t = buildInstance(constructor, args);

        if (singleton) {
            singletonCache.put(resolvedClass, t);
        }

        return t;
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
        return clazz.isAnnotationPresent(Singleton.class);
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

    private Instance<?> createInstanceWrapper(java.lang.reflect.Parameter param) {
        // Extract the generic type T from Instance<T>
        ParameterizedType type = (ParameterizedType) param.getParameterizedType();
        Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
        boolean isAny = param.isAnnotationPresent(Any.class);
        Alternative alternative = param.getAnnotation(Alternative.class);
        String identifier = alternative != null ? alternative.value() : null;

        return new Instance<Object>() {
            @Override
            public Object get() throws Exception {
                return inject(genericType, identifier);
            }

            @Override
            public @NonNull Iterator<Object> iterator() {
                try {
                    Collection<? extends Class<?>> classes = isAny
                            ? classResolver.resolveImplementations(genericType, packageName)
                            : Collections.singletonList(classResolver.resolveImplementation(genericType, packageName, identifier));

                    List<Object> instances = new ArrayList<>();
                    for (Class<?> clazz : classes) {
                        instances.add(inject(clazz));
                    }
                    return instances.iterator();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to resolve implementations for @Any", e);
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
