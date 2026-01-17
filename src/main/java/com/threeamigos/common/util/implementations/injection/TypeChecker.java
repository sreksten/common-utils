package com.threeamigos.common.util.implementations.injection;

import javax.enterprise.inject.spi.DefinitionException;
import java.lang.reflect.*;

class TypeChecker {

    /**
     * A cache for storing the results of type assignability checks.
     */
    private final Cache<TypePair, Boolean> assignabilityCache = new Cache<>();

    /**
     * Validates that a type is a legal bean type for an injection point.
     * Per JSR 330/346, injection points cannot contain wildcards or type variables.
     *
     * @param type the type to validate
     * @throws DefinitionException if the type contains wildcards or type variables
     */
    void validateInjectionPoint(Type type) {
        if (type instanceof WildcardType) {
            throw new DefinitionException("Injection point cannot contain a wildcard: " + type.getTypeName());
        }

        if (type instanceof TypeVariable) {
            throw new DefinitionException("Injection point cannot be a type variable: " + type.getTypeName());
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            for (Type arg : pt.getActualTypeArguments()) {
                validateInjectionPoint(arg); // Recursive check
            }
        }

        if (type instanceof GenericArrayType) {
            validateInjectionPoint(((GenericArrayType) type).getGenericComponentType());
        }
    }

    /**
     * Checks if an implementation type can be assigned to a target type, following
     * Java's type system rules including generics covariance.
     *
     * <p>This method validates that the target type is a legal injection point
     * (no wildcards or type variables per JSR 330/346), then checks assignability
     * considering:
     * <ul>
     *   <li>Raw type assignability (e.g., ArrayList → List)</li>
     *   <li>Generic type argument matching (e.g., List&lt;String&gt; → List&lt;String&gt;)</li>
     *   <li>Covariance in generics (e.g., ArrayList&lt;String&gt; → List&lt;String&gt;)</li>
     *   <li>Array component type assignability</li>
     * </ul>
     *
     * <p>Results are cached for performance.
     *
     * <p>Example:
     * <pre>
     * Type target = new TypeLiteral&lt;List&lt;String&gt;&gt;() {}.getType();
     * Type impl = new TypeLiteral&lt;ArrayList&lt;String&gt;&gt;() {}.getType();
     * boolean result = checker.isAssignable(target, impl); // returns true
     * </pre>
     *
     * @param targetType the type required by an injection point (must not contain wildcards)
     * @param implementationType the type of candidate bean to inject
     * @return true if implementationType can be assigned to targetType
     * @throws DefinitionException if targetType contains wildcards or type variables
     * @throws IllegalStateException if type hierarchy navigation fails unexpectedly
     */
    boolean isAssignable(Type targetType, Type implementationType) {
        TypePair pair = new TypePair(targetType, implementationType);
        return assignabilityCache.computeIfAbsent(pair, () -> isAssignableInternal(targetType, implementationType));
    }

    boolean isAssignableInternal(Type targetType, Type implementationType) {
        validateInjectionPoint(targetType);

        if (targetType.equals(implementationType)) {
            return true;
        }

        Class<?> targetRaw = RawTypeExtractor.getRawType(targetType);
        Class<?> implementationRaw = RawTypeExtractor.getRawType(implementationType);

        if (!targetRaw.isAssignableFrom(implementationRaw)) {
            return false;
        }

        if (targetType instanceof Class<?>) {
            return true;
        }

        if (targetType instanceof ParameterizedType) {
            // Find how implementationType fulfills targetRaw (e.g., ArrayList<Integer> -> List<Integer>)
            Type exactSuperType = getExactSuperType(implementationType, targetRaw);
            if (exactSuperType == null) {
                throw new IllegalStateException(
                    "getExactSuperType returned null despite isAssignableFrom being true. " +
                    "Target: " + targetType + ", Implementation: " + implementationType);
            }
            return typesMatch(targetType, exactSuperType);
        }

        if (targetType instanceof GenericArrayType) {
            if (!implementationRaw.isArray()) {
                throw new IllegalStateException(
                    "Implementation type is not an array despite passing isAssignableFrom check. " +
                    "Target: " + targetType + " (raw: " + targetRaw + "), " +
                    "Implementation: " + implementationType + " (raw: " + implementationRaw + ")");
            }
            Type targetComponent = ((GenericArrayType) targetType).getGenericComponentType();
            return isAssignable(targetComponent, implementationRaw.getComponentType());
        }

        throw new IllegalStateException(
            "Unexpected target type: " + targetType.getClass().getName() +
            " - " + targetType + ". Expected Class, ParameterizedType, or GenericArrayType.");
    }

    Type getExactSuperType(Type type, Class<?> targetRaw) {
        Class<?> raw = RawTypeExtractor.getRawType(type);
        if (raw == targetRaw) return type;

        if (targetRaw.isInterface()) {
            for (Type itf : raw.getGenericInterfaces()) {
                Type resolvedItf = resolveTypeVariables(itf, type);
                Type result = getExactSuperType(resolvedItf, targetRaw);
                if (result != null) return result;
            }
        }

        Type superType = raw.getGenericSuperclass();
        if (superType != null && superType != Object.class) {
            Type resolvedSuper = resolveTypeVariables(superType, type);
            return getExactSuperType(resolvedSuper, targetRaw);
        }
        return null;
    }

    Type resolveTypeVariables(Type toResolve, Type context) {
        if (!(toResolve instanceof ParameterizedType) || !(context instanceof ParameterizedType)) {
            return toResolve;
        }
        ParameterizedType pt = (ParameterizedType) toResolve;
        ParameterizedType contextPt = (ParameterizedType) context;
        Class<?> contextRaw = (Class<?>) contextPt.getRawType();
        TypeVariable<?>[] vars = contextRaw.getTypeParameters();
        Type[] args = pt.getActualTypeArguments().clone();
        boolean changed = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof TypeVariable) {
                TypeVariable<?> tv = (TypeVariable<?>) args[i];
                boolean found = false;
                for (int j = 0; j < vars.length; j++) {
                    if (vars[j].getName().equals(tv.getName())) {
                        args[i] = contextPt.getActualTypeArguments()[j];
                        changed = true;
                        found = true;
                        break;
                    }
                }
                // Defensive check - should never happen in valid Java type hierarchies
                if (!found) {
                    // Log warning or throw assertion error in development
                    throw new IllegalStateException("TypeVariable " + tv.getName() +
                            " not found in context type parameters");
                }
            }
        }
        if (!changed) return toResolve;
        return new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { return args; }
            @Override public Type getRawType() { return pt.getRawType(); }
            @Override public Type getOwnerType() { return pt.getOwnerType(); }
        };
    }

    boolean typesMatch(Type target, Type candidate) {
        if (target.equals(candidate)) {
            return true;
        }

        if (target instanceof ParameterizedType && candidate instanceof ParameterizedType) {
            ParameterizedType pt1 = (ParameterizedType) target;
            ParameterizedType pt2 = (ParameterizedType) candidate;

            if (!pt1.getRawType().equals(pt2.getRawType())) {
                return false;
            }

            return actualTypeArgumentsMatch(pt1, pt2);
        }
        return false;
    }

    boolean typeArgsMatch(Type t1, Type t2) {
        if (t1.equals(t2)) {
            return true;
        }

        // t1 (target) cannot be Wildcard or TypeVariable due to validateInjectionPoint.
        // But t2 (candidate/implementation) can be.
        if (t2 instanceof WildcardType || t2 instanceof TypeVariable) {
            return true;
        }

        // For nested parameterized types, we need to resolve t2 to t1's raw type structure
        if (t1 instanceof ParameterizedType && t2 instanceof ParameterizedType) {
            return matchParameterizedTypes(t1, t2);
        }

        // Handle raw type (Class) in t1 vs ParameterizedType in t2
        if (t1 instanceof Class<?> && t2 instanceof ParameterizedType) {
            Class<?> raw1 = (Class<?>) t1;
            ParameterizedType pt2 = (ParameterizedType) t2;
            Class<?> raw2 = (Class<?>) pt2.getRawType();
            return raw1.isAssignableFrom(raw2);
        }

        // Handle ParameterizedType in t1 vs raw type (Class) in t2
        if (t1 instanceof ParameterizedType && t2 instanceof Class<?>) {
            ParameterizedType pt1 = (ParameterizedType) t1;
            Class<?> raw1 = (Class<?>) pt1.getRawType();
            Class<?> raw2 = (Class<?>) t2;
            return raw1.isAssignableFrom(raw2);
        }
        // For non-parameterized types, use exact equality (invariance)
        return t1.equals(t2);
    }

    private boolean matchParameterizedTypes(Type t1, Type t2) {
        ParameterizedType pt1 = (ParameterizedType) t1;
        ParameterizedType pt2 = (ParameterizedType) t2;

        Class<?> raw1 = (Class<?>) pt1.getRawType();
        Class<?> raw2 = (Class<?>) pt2.getRawType();

        // If raw types match exactly, check type arguments recursively
        if (raw1.equals(raw2)) {
            return actualTypeArgumentsMatch(pt1, pt2);
        }

        // If raw types differ but are assignable, resolve t2 to t1's raw type
        if (raw1.isAssignableFrom(raw2)) {
            Type resolvedT2 = getExactSuperType(t2, raw1);
            if (resolvedT2 == null) {
                throw new IllegalStateException(
                        "getExactSuperType returned null despite isAssignableFrom being true in typeArgsMatch. " +
                                "t1: " + t1 + " (raw1: " + raw1 + "), t2: " + t2 + " (raw2: " + raw2 + ")");
            }
            return typeArgsMatch(t1, resolvedT2);
        }

        return false;
    }

    boolean actualTypeArgumentsMatch(ParameterizedType pt1, ParameterizedType pt2) {
        Type[] args1 = pt1.getActualTypeArguments();
        Type[] args2 = pt2.getActualTypeArguments();

        if (args1.length != args2.length) {
            return false;
        }

        for (int i = 0; i < args1.length; i++) {
            if (!typeArgsMatch(args1[i], args2[i])) {
                return false;
            }
        }
        return true;
    }

}
