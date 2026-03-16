package com.threeamigos.common.util.implementations.injection.util;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashSet;
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
        addClassHierarchy(types, beanClass);
        types.add(Object.class);
        return types;
    }

    /**
     * Returns the unrestricted type closure for a producer type.
     */
    public static Set<Type> extractTypesFromType(Type baseType) {
        Objects.requireNonNull(baseType, "baseType cannot be null");

        Set<Type> types = new LinkedHashSet<>();
        types.add(baseType);
        addClassHierarchy(types, RawTypeExtractor.getRawType(baseType));
        types.add(Object.class);
        return types;
    }

    private static void addClassHierarchy(Set<Type> types, Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            types.add(current);
            types.addAll(Arrays.asList(current.getGenericInterfaces()));
            current = current.getSuperclass();
        }
    }
}
