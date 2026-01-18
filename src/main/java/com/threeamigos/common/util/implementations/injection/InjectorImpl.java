package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.interfaces.injection.Injector;
import com.threeamigos.common.util.interfaces.injection.ScopeHandler;
import org.jspecify.annotations.NonNull;

import javax.enterprise.inject.*;
import javax.enterprise.util.TypeLiteral;
import javax.inject.*;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.Closeable;
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
     * A stack pool for thread-local storage of type stacks. Each thread has its own stack to avoid contention.
     */
    private static final ThreadLocal<Stack<Type>> STACK_POOL = ThreadLocal.withInitial(Stack::new);

    /**
     * Internal registry for scope handlers. By default, it contains only the SingletonScopeHandler.
     * Should be set up during initialization and should not be modified after that.
     */
    private final Map<Class<? extends Annotation>, ScopeHandler> scopeRegistry = new ConcurrentHashMap<>();

    /**
     * The class resolver used in this injector to find concrete implementations of abstract classes and interfaces.
     */
    private final ClassResolver classResolver;

    /**
     * Tracks classes that have already had their static members injected to prevent redundant injections.
     */
    private final Set<Class<?>> injectedStaticClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Package names are used to restrict classes search to a particular package(s). If you don't want to
     * scan the whole classpath, you can use your package name here. Beware - all your interfaces and
     * implementations should reside in that package or subpackages!
     */
    public InjectorImpl(final String ... packageNames) {
        this.classResolver = new ClassResolver(packageNames);
        registerDefaultScope();
        addShutdownHook();
    }

    InjectorImpl(final ClassResolver classResolver) {
        this.classResolver = classResolver;
        registerDefaultScope();
        addShutdownHook();
    }

    private void registerDefaultScope() {
        // Standard Singleton scope implementation
        scopeRegistry.put(Singleton.class, new ScopeHandler() {
            private final Map<Class<?>, Object> instances = new ConcurrentHashMap<>();
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(Class<T> clazz, Supplier<T> provider) {
                // We don't use computeIfAbsent to handle concurrency issues, when A depends on B and
                // both are Singletons
                Object instance = instances.get(clazz);
                if (instance == null) {
                    synchronized (instances) {
                        instance = instances.get(clazz);
                        if (instance == null) {
                            instance = provider.get();
                            instances.put(clazz, instance);
                        }
                    }
                }
                return (T) instance;
            }

            @Override
            public void close() throws Exception {
                // Call @PreDestroy on all singletons
                for (Object instance : instances.values()) {
                    try {
                        invokePreDestroy(instance);
                    } catch (Exception e) {
                        // Log but continue destroying others
                    }
                }
                instances.clear();
            }
        });
    }

    void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    public void registerScope(@NonNull Class<? extends Annotation> scopeAnnotation, @NonNull ScopeHandler handler) {
        scopeRegistry.put(scopeAnnotation, handler);
    }

    @Override
    public void enableAlternative(@NonNull Class<?> alternativeClass) {
        classResolver.enableAlternative(alternativeClass);
    }

    @Override
    public void bind(@NonNull Type type, @NonNull Collection<Annotation> qualifiers, @NonNull Class<?> implementation) {
        classResolver.bind(type, qualifiers, implementation);
    }

    @Override
    public <T> T inject(@NonNull Class<T> classToInject) {
        return inject(classToInject, new Stack<>(), null);
    }

    @Override
    public <T> T inject(@NonNull TypeLiteral<T> typeLiteral) {
        Stack<Type> stack = STACK_POOL.get();
        try {
            stack.clear();
            return inject(typeLiteral.getType(), new Stack<>(), null);
        } finally {
            stack.clear();
        }
    }

    private <T> T inject(@NonNull Type typeToInject, Stack<Type> stack, Collection<Annotation> qualifiers) {
        if (stack.contains(typeToInject)) {
            stack.add(typeToInject);
            throw new InjectionException("Circular dependency detected for class " +
                    typeToInject.getClass().getName() + ": " +
                    stack.stream().map(Type::getTypeName).collect(Collectors.joining(" -> ")));
        }
        stack.push(typeToInject);
        try {
            checkClassValidity(typeToInject);
            Class<? extends T> resolvedClass = classResolver.resolveImplementation(typeToInject, qualifiers);

            // Find the scope annotation on the resolved class
            Class<? extends Annotation> scopeType = getScopeType(resolvedClass);

            if (scopeType != null && scopeRegistry.containsKey(scopeType)) {
                ScopeHandler handler = scopeRegistry.get(scopeType);
                // A helper method to handle the wildcard capture safely
                T t = handleScopedInjection(handler, typeToInject, resolvedClass, stack);
                stack.pop();
                return t;
            }

            T t = performInjection(typeToInject, resolvedClass, stack);
            stack.pop();
            return t;
        } catch (InjectionException e) {
            throw e;
        } catch (Exception e) {
            String injectionPath = stack.stream()
                    .map(Type::getTypeName)
                    .collect(Collectors.joining(" -> "));
            throw new InjectionException("Failed to inject " + RawTypeExtractor.getRawType(typeToInject).getName() +
                    "\nInjection path: " + injectionPath +
                    "\nCause: " + e.getMessage(), e);
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
    private <T> T handleScopedInjection(ScopeHandler handler, Type typeContext, Class<? extends T> clazz, Stack<Type> stack) {
        return (T) handler.get((Class<Object>) clazz, () -> performInjection(typeContext, clazz, stack));
    }

    /**
     * The actual instantiation logic
     */
    private <T> T performInjection(Type typeContext, Class<? extends T> resolvedClass, Stack<Type> stack) {
        try {
            if (resolvedClass.isArray()) {
                return (T) Array.newInstance(resolvedClass.getComponentType(), 0);
            }
            Constructor<? extends T> constructor = getConstructor(resolvedClass);
            Parameter[] parameters = constructor.getParameters();
            validateConstructorParameters(parameters, resolvedClass);
            Object[] args = resolveParameters(typeContext, parameters, stack);
            T t = buildInstance(constructor, args);

            // Collect all classes in the hierarchy, from top to bottom
            List<Class<?>> hierarchy = new ArrayList<>();
            Class<?> current = t.getClass();
            while (current != Object.class) {
                hierarchy.add(0, current); // Add as first, so we start processing from parent classes
                current = current.getSuperclass();
            }

            for (Class<?> clazz : hierarchy) {
                injectFields(t, typeContext, stack, clazz, resolvedClass, true);
            }
            for (Class<?> clazz : hierarchy) {
                injectMethods(t, typeContext, stack, clazz, resolvedClass, true);
                // After processing fields and methods for this specific class in the hierarchy,
                // mark it as static-injected.
                injectedStaticClasses.add(clazz);
            }
            for (Class<?> clazz : hierarchy) {
                injectFields(t, typeContext, stack, clazz, resolvedClass, false);
                injectMethods(t, typeContext, stack, clazz, resolvedClass, false);
            }

            // Call @PostConstruct methods
            invokePostConstruct(t);

            return t;
        } catch (Exception e) {
            throw new InjectionException("Injection failed for " + resolvedClass.getName() + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> getConstructor(@NonNull Class<T> clazz) {
        List<Constructor<T>> constructors = Arrays.stream((Constructor<T>[])clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (constructors.size() > 1) {
            throw new InjectionException("More than one constructor annotated with @Inject in class " + clazz.getName());
        } else if (constructors.size() == 1) {
            return constructors.get(0);
        }
        // No @Inject constructor found, let's try to find a no-argument constructor
        constructors = Arrays.stream((Constructor<T>[])clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .collect(Collectors.toList());
        if (constructors.isEmpty()) {
            throw new InjectionException("No empty constructor or a constructor annotated with @Inject in class " + clazz.getName());
        }
        return constructors.get(0);
    }

    private void validateConstructorParameters(Parameter[] parameters, Class<?> clazz) {
        for (Parameter param : parameters) {
            try {
                checkClassValidity(param.getType());
            } catch (IllegalArgumentException e) {
                throw new InjectionException("Cannot inject into constructor parameter " + param.getName() + " of class " +
                        clazz.getName() + ": " + e.getMessage());
            }
        }
    }

    private void validateMethodParameters(Parameter[] parameters, String methodName, Class<?> clazz) {
        for (Parameter param : parameters) {
            try {
                checkClassValidity(param.getType());
            } catch (IllegalArgumentException e) {
                throw new InjectionException("Cannot inject into parameter " + param.getName() + " of method " +
                        clazz.getName() + "::" + methodName + ": " + e.getMessage());
            }
        }
    }

    private Object[] resolveParameters(Type typeContext, Parameter[] parameters, Stack<Type> stack) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (Provider.class.isAssignableFrom(param.getType())) {
                args[i] = createInstanceWrapper(param);
            } else {
                Type paramType = resolveType(param.getParameterizedType(), typeContext);
                checkClassValidity(paramType);
                Collection<Annotation> paramQualifiers = getQualifiers(param);
                args[i] = inject(paramType, stack, paramQualifiers);
            }
        }
        return args;
    }

    private Type resolveType(Type toResolve, Type context) {
        if (!(toResolve instanceof TypeVariable)) {
            return toResolve;
        }
        TypeVariable<?> tv = (TypeVariable<?>) toResolve;
        if (context instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) context;
            Class<?> raw = (Class<?>) pt.getRawType();
            TypeVariable<?>[] vars = raw.getTypeParameters();
            for (int i = 0; i < vars.length; i++) {
                if (vars[i].getName().equals(tv.getName())) {
                    return pt.getActualTypeArguments()[i];
                }
            }
        }
        return toResolve;
    }

    <T> T buildInstance(Constructor<? extends T> constructor, Object... args) throws Exception {
        if (!Modifier.isPublic(constructor.getModifiers())) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance(args);
    }

    private <T> void injectFields(T t, Type typeContext, Stack<Type> stack, Class<?> clazz, Class<?> resolvedClass, boolean onlyStatic) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        boolean isStaticAlreadyInjected = injectedStaticClasses.contains(clazz);

        for (Field field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                boolean isStatic = Modifier.isStatic(field.getModifiers());

                if (onlyStatic != isStatic) {
                    continue;
                }

                if (isStatic && isStaticAlreadyInjected) {
                    continue;
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new IllegalStateException("Cannot inject into final field " + field.getName() + " of class " +
                            resolvedClass.getName());
                }
                field.setAccessible(true);
                if (Provider.class.isAssignableFrom(field.getType())) {
                    field.set(t, createInstanceWrapper(field));
                } else {
                    Type fieldType = resolveType(field.getGenericType(), typeContext);
                    checkClassValidity(fieldType);
                    Collection<Annotation> fieldQualifiers = getQualifiers(field);
                    field.set(t, inject(fieldType, stack, fieldQualifiers));
                }
            }
        }
    }

    private<T> void injectMethods(T t, Type typeContext, Stack<Type> stack, Class<?> clazz, Class<?> resolvedClass, boolean onlyStatic) throws Exception {
        Method[] methods = clazz.getDeclaredMethods();
        boolean isStaticAlreadyInjected = injectedStaticClasses.contains(clazz);

        for (Method method : methods) {
            if (method.isAnnotationPresent(Inject.class)) {
                boolean isStatic = Modifier.isStatic(method.getModifiers());

                if (onlyStatic != isStatic) {
                    continue;
                }

                if (isStatic && isStaticAlreadyInjected) {
                    continue;
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    throw new InjectionException("Cannot inject into abstract method " + method.getName() + " of class " +
                            resolvedClass.getName());
                }

                if (method.getTypeParameters().length > 0) {
                    throw new InjectionException("Cannot inject into generic method " + method.getName() + " of class " +
                            resolvedClass.getName());
                }

                // JSR-330: Only inject the most specific override.
                // If this method is overridden by a subclass, skip it here.
                // It will be handled when the loop reaches that subclass.
                if (isOverridden(method, t.getClass())) {
                    continue;
                }

                method.setAccessible(true);
                Parameter[] parameters = method.getParameters();
                validateMethodParameters(parameters, method.getName(), resolvedClass);
                Object[] params = resolveParameters(typeContext, parameters, stack);
                method.invoke(t, params);
            }
        }
    }

    private void invokePostConstruct(Object instance) throws InvocationTargetException, IllegalAccessException {
        LifecycleMethodHelper.invokeLifecycleMethod(instance, PostConstruct.class);
    }

   private void invokePreDestroy(Object instance) throws InvocationTargetException, IllegalAccessException {
       LifecycleMethodHelper.invokeLifecycleMethod(instance, PreDestroy.class);
    }

    private boolean isOverridden(Method superMethod, Class<?> leafClass) {
        if (Modifier.isPrivate(superMethod.getModifiers())) {
            return false;
        }
        if (superMethod.getDeclaringClass().equals(leafClass)) {
            return false;
        }

        Method subMethod = findMethod(leafClass, superMethod.getName(), superMethod.getParameterTypes());
        if (subMethod == null || subMethod.equals(superMethod)) {
            return false;
        }

        // Check JSR-330 package-private rules:
        // Package-private methods can only override if they're in the same package
        boolean isSuperPackagePrivate = !Modifier.isPublic(superMethod.getModifiers()) &&
                !Modifier.isProtected(superMethod.getModifiers()) &&
                !Modifier.isPrivate(superMethod.getModifiers());

        if (isSuperPackagePrivate) {
            // Package-private method is only overridden if subclass method is in the same package
            return getPackageName(superMethod.getDeclaringClass())
                    .equals(getPackageName(subMethod.getDeclaringClass()));
        }

        return true;
    }

    private String getPackageName(Class<?> clazz) {
        String name = clazz.getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot == -1) ? "" : name.substring(0, lastDot);
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Checks that the class is valid for injection.
     * @param type the type to check
     */
    private void checkClassValidity(Type type) {
        Class<?> clazz = RawTypeExtractor.getRawType(type);

        // 1. Basic JSR 330 / Java constraints
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

        // 2. Recursive validation for Parameterized Types (Generics)
        // This ensures Holder<MyEnum> is rejected if MyEnum is not injectable
        if (type instanceof ParameterizedType) {
            for (Type arg : ((ParameterizedType) type).getActualTypeArguments()) {
                // We only validate classes/types that the injector would actually try to resolve
                if (arg instanceof Class<?> || arg instanceof ParameterizedType) {
                    checkClassValidity(arg);
                }
            }
        }
    }

    private Collection<Annotation> getQualifiers(Parameter param) {
        Collection<Annotation> qualifiers = Arrays.stream(param.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .collect(Collectors.toList());
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        return qualifiers;
    }

    private Collection<Annotation> getQualifiers(Field field) {
        Collection<Annotation> qualifiers = Arrays.stream(field.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                .collect(Collectors.toList());
        if (qualifiers.isEmpty()) {
            qualifiers.add(new DefaultLiteral());
        }
        return qualifiers;
    }

    private Instance<?> createInstanceWrapper(Field field) {
        ParameterizedType type = (ParameterizedType) field.getGenericType();
        Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
        Collection<Annotation> qualifiers = getQualifiers(field);
        return createInstance(genericType, qualifiers);
    }

    private Instance<?> createInstanceWrapper(Parameter param) {
        ParameterizedType type = (ParameterizedType) param.getParameterizedType();
        Class<?> genericType = (Class<?>) type.getActualTypeArguments()[0];
        Collection<Annotation> qualifiers = getQualifiers(param);
        return createInstance(genericType, qualifiers);
    }

    private <T> Instance<T> createInstance(Class<T> type, Collection<Annotation> qualifiers) {
        return new Instance<T>() {
            @Override
            public T get() {
                try {
                    return inject(type, new Stack<>(), qualifiers);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to inject " + type.getName(), e);
                }
            }

            @Override
            public Instance<T> select(Annotation... annotations) {
                return createInstance(type, mergeQualifiers(qualifiers, annotations));
            }

            @Override
            public <U extends T> Instance<U> select(Class<U> subtype, Annotation... annotations) {
                return createInstance(subtype, mergeQualifiers(qualifiers, annotations));
            }

            @Override
            public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... annotations) {
                // We extract the raw class from the TypeLiteral to maintain compatibility with createInstance
                @SuppressWarnings("unchecked")
                Class<U> rawType = (Class<U>) RawTypeExtractor.getRawType(subtype.getType());
                return createInstance(rawType, mergeQualifiers(qualifiers, annotations));
            }

            @Override
            public boolean isUnsatisfied() {
                try {
                    return classResolver.resolveImplementations(type).isEmpty();
                } catch (Exception e) {
                    return true; // treating an Exception as unsatisfied
                }
            }

            @Override
            public boolean isAmbiguous() {
                try {
                    return classResolver.resolveImplementations(type).size() > 1;
                } catch (Exception e) {
                    return false; // If we can't resolve the class, it's not ambiguous (it's unsatisfied)
                }
            }

            @Override
            public void destroy(T instance) {
                try {
                    if (instance != null) {
                        invokePreDestroy(instance);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke @PreDestroy on " + type.getName(), e);
                }
            }

            @Override
            public @NonNull Iterator<T> iterator() {
                try {
                    Collection<Class<? extends T>> classes = classResolver.resolveImplementations(type, qualifiers);

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

    Collection<Annotation> mergeQualifiers(Collection<Annotation> existing, Annotation... newAnnotations) {
        if (newAnnotations == null || newAnnotations.length == 0) {
            return existing;
        }

        Map<Class<? extends Annotation>, Annotation> merged = new HashMap<>();
        // Start with existing
        for (Annotation a : existing) {
            merged.put(a.annotationType(), a);
        }
        // Overwrite/Add new ones
        for (Annotation a : newAnnotations) {
            merged.put(a.annotationType(), a);
        }

        // If we now have specific qualifiers, remove the @Default literal if it exists
        if (merged.size() > 1) {
            merged.remove(Default.class);
        }

        return new ArrayList<>(merged.values());
    }

    /**
     * Clears all internal state, including scope handlers and injected static classes.
     */
    void clearState() {
        scopeRegistry.clear();
        registerDefaultScope();
        injectedStaticClasses.clear();
    }

    /**
     * Returns all registered scope annotations.
     */
    public Set<Class<? extends Annotation>> getRegisteredScopes() {
        return Collections.unmodifiableSet(scopeRegistry.keySet());
    }

    /**
     * Checks if a scope is registered.
     */
    public boolean isScopeRegistered(Class<? extends Annotation> scopeAnnotation) {
        return scopeRegistry.containsKey(scopeAnnotation);
    }

    /**
     * Unregisters a scope handler.
     */
    public void unregisterScope(Class<? extends Annotation> scopeAnnotation) {
        scopeRegistry.remove(scopeAnnotation);
    }

    @Override
    public void shutdown() {
        // Notify custom scopes
        for (ScopeHandler handler : scopeRegistry.values()) {
            if (handler != null) {
                try {
                    handler.close();
                } catch (Exception e) {
                    // Log but continue
                }
            }
        }
    }
}
