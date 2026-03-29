package com.threeamigos.common.util.implementations.injection.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared helpers for extracting unrestricted type closures used by CDI components.
 */
public final class TypeClosureHelper {

    private TypeClosureHelper() {}

    /**
     * Returns the unrestricted type closure for a managed bean class.
     */
    public static Set<Type> extractTypesFromClass(Class<?> beanClass) {
        Objects.requireNonNull(beanClass, "beanClass cannot be null");

        Set<Type> types = new LinkedHashSet<>();
        collectTypeClosure(beanClass, types, new HashMap<>(), new LinkedHashSet<>());
        types.add(Object.class);
        return types;
    }

    /**
     * Returns the unrestricted type closure for a producer type.
     */
    public static Set<Type> extractTypesFromType(Type baseType) {
        Objects.requireNonNull(baseType, "baseType cannot be null");

        Set<Type> types = new LinkedHashSet<>();
        Class<?> rawType = RawTypeExtractor.getRawType(baseType);

        // CDI 4.1 §3.2.1: for primitive and Java array producer return types,
        // unrestricted bean types are exactly the return type and Object.
        if (rawType.isPrimitive() || rawType.isArray()) {
            types.add(baseType);
            types.add(Object.class);
            return types;
        }

        collectTypeClosure(baseType, types, new HashMap<>(), new LinkedHashSet<>());
        types.add(Object.class);
        return types;
    }

    private static void collectTypeClosure(Type type, Set<Type> types, Map<TypeVariable<?>, Type> inheritedBindings, Set<String> visited) {
        Type resolvedType = resolveType(type, inheritedBindings);
        Class<?> rawType = RawTypeExtractor.getRawType(resolvedType);

        if (rawType == Object.class) {
            return;
        }

        String visitKey = resolvedType.getTypeName() + "@" + rawType.getName();
        if (!visited.add(visitKey)) {
            return;
        }

        types.add(toBeanType(resolvedType, rawType));

        Map<TypeVariable<?>, Type> currentBindings = new HashMap<>(inheritedBindings);
        if (resolvedType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) resolvedType;
            TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < typeParameters.length && i < actualTypeArguments.length; i++) {
                currentBindings.put(typeParameters[i], actualTypeArguments[i]);
            }
        }

        for (Type interfaceType : rawType.getGenericInterfaces()) {
            collectTypeClosure(interfaceType, types, currentBindings, visited);
        }

        Type genericSuperclass = rawType.getGenericSuperclass();
        if (genericSuperclass != null) {
            collectTypeClosure(genericSuperclass, types, currentBindings, visited);
        }
    }

    private static Type toBeanType(Type resolvedType, Class<?> rawType) {
        if (resolvedType instanceof ParameterizedType) {
            return resolvedType;
        }
        return rawType;
    }

    private static Type resolveType(Type type, Map<TypeVariable<?>, Type> bindings) {
        if (type instanceof TypeVariable<?>) {
            Type resolved = bindings.get(type);
            return resolved != null ? resolveType(resolved, bindings) : type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type[] originalArgs = pt.getActualTypeArguments();
            Type[] resolvedArgs = new Type[originalArgs.length];
            boolean changed = false;
            for (int i = 0; i < originalArgs.length; i++) {
                resolvedArgs[i] = resolveType(originalArgs[i], bindings);
                if (!resolvedArgs[i].equals(originalArgs[i])) {
                    changed = true;
                }
            }
            Type owner = pt.getOwnerType();
            Type resolvedOwner = owner == null ? null : resolveType(owner, bindings);
            if (resolvedOwner != null && !resolvedOwner.equals(owner)) {
                changed = true;
            }
            return changed ? new SimpleParameterizedType((Class<?>) pt.getRawType(), resolvedArgs, resolvedOwner) : pt;
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            Type originalComponent = gat.getGenericComponentType();
            Type resolvedComponent = resolveType(originalComponent, bindings);
            return resolvedComponent.equals(originalComponent) ? gat : new SimpleGenericArrayType(resolvedComponent);
        }
        if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type[] lower = wt.getLowerBounds();
            Type[] upper = wt.getUpperBounds();
            Type[] resolvedLower = new Type[lower.length];
            Type[] resolvedUpper = new Type[upper.length];
            boolean changed = false;
            for (int i = 0; i < lower.length; i++) {
                resolvedLower[i] = resolveType(lower[i], bindings);
                if (!resolvedLower[i].equals(lower[i])) {
                    changed = true;
                }
            }
            for (int i = 0; i < upper.length; i++) {
                resolvedUpper[i] = resolveType(upper[i], bindings);
                if (!resolvedUpper[i].equals(upper[i])) {
                    changed = true;
                }
            }
            return changed ? new SimpleWildcardType(resolvedUpper, resolvedLower) : wt;
        }
        return type;
    }

    private static final class SimpleParameterizedType implements ParameterizedType {
        private final Class<?> rawType;
        private final Type[] actualTypeArguments;
        private final Type ownerType;

        private SimpleParameterizedType(Class<?> rawType, Type[] actualTypeArguments, Type ownerType) {
            this.rawType = rawType;
            this.actualTypeArguments = actualTypeArguments.clone();
            this.ownerType = ownerType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments.clone();
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ParameterizedType)) {
                return false;
            }
            ParameterizedType that = (ParameterizedType) o;
            return Objects.equals(rawType, that.getRawType())
                    && Objects.equals(ownerType, that.getOwnerType())
                    && Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(actualTypeArguments)
                    ^ Objects.hashCode(ownerType)
                    ^ Objects.hashCode(rawType);
        }
    }

    private static final class SimpleGenericArrayType implements GenericArrayType {
        private final Type componentType;

        private SimpleGenericArrayType(Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GenericArrayType)) {
                return false;
            }
            GenericArrayType that = (GenericArrayType) o;
            return Objects.equals(componentType, that.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(componentType);
        }
    }

    private static final class SimpleWildcardType implements WildcardType {
        private final Type[] upperBounds;
        private final Type[] lowerBounds;

        private SimpleWildcardType(Type[] upperBounds, Type[] lowerBounds) {
            this.upperBounds = upperBounds.clone();
            this.lowerBounds = lowerBounds.clone();
        }

        @Override
        public Type[] getUpperBounds() {
            return upperBounds.clone();
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WildcardType)) {
                return false;
            }
            WildcardType that = (WildcardType) o;
            return Arrays.equals(upperBounds, that.getUpperBounds())
                    && Arrays.equals(lowerBounds, that.getLowerBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
        }
    }
}
