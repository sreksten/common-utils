package com.threeamigos.common.util.implementations.injection;

import java.lang.reflect.*;

class TypeChecker {

    boolean isAssignable(Type targetType, Type implementationType) {

        if (targetType.equals(implementationType)) {
            return true;
        }

        // Logic for Intersection Types (Multiple Bounds) via TypeVariable
        if (targetType instanceof TypeVariable) {
            for (Type bound : ((TypeVariable<?>) targetType).getBounds()) {
                if (!isAssignable(bound, implementationType)) {
                    return false;
                }
            }
            return true;
        }

        // Handle Wildcards
        if (targetType instanceof WildcardType) {
            WildcardType wt = (WildcardType) targetType;
            for (Type bound : wt.getUpperBounds()) {
                if (!isAssignable(bound, implementationType)) {
                    return false;
                }
            }
            for (Type bound : wt.getLowerBounds()) {
                if (!isAssignable(implementationType, bound)) {
                    return false;
                }
            }
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
                return true; // Fallback for raw types
            }
            return typesMatch(targetType, exactSuperType);
        }

        if (targetType instanceof GenericArrayType) {
            if (!implementationRaw.isArray()) return false;
            Type targetComponent = ((GenericArrayType) targetType).getGenericComponentType();
            return isAssignable(targetComponent, implementationRaw.getComponentType());
        }

        return false;
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
                for (int j = 0; j < vars.length; j++) {
                    if (vars[j].getName().equals(tv.getName())) {
                        args[i] = contextPt.getActualTypeArguments()[j];
                        changed = true;
                        break;
                    }
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

        // If the target is a TypeVariable (like T), it matches its bound (usually Object)
        if (target instanceof TypeVariable) {
            return typeArgsMatch(target, candidate);
        }

        if (target instanceof ParameterizedType && candidate instanceof ParameterizedType) {
            ParameterizedType pt1 = (ParameterizedType) target;
            ParameterizedType pt2 = (ParameterizedType) candidate;

            if (!pt1.getRawType().equals(pt2.getRawType())) {
                return false;
            }

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
        return false;
    }

    boolean typeArgsMatch(Type t1, Type t2) {
        if (t1.equals(t2)) {
            return true;
        }

        if (t1 instanceof WildcardType && t2 instanceof WildcardType) {
            return wildcardsMatch((WildcardType) t1, (WildcardType) t2);
        }

        if (t1 instanceof TypeVariable && t2 instanceof TypeVariable) {
            return typeVariablesMatch((TypeVariable<?>) t1, (TypeVariable<?>) t2);
        }

        if (t1 instanceof WildcardType ||
                t2 instanceof WildcardType ||
                t1 instanceof TypeVariable ||
                t2 instanceof TypeVariable) {
            return true;
        }
        return RawTypeExtractor.getRawType(t1).isAssignableFrom(RawTypeExtractor.getRawType(t2));
    }

    private boolean wildcardsMatch(WildcardType w1, WildcardType w2) {
        return boundsMatch(w1.getUpperBounds(), w2.getUpperBounds()) &&
                boundsMatch(w1.getLowerBounds(), w2.getLowerBounds());
    }

    private boolean typeVariablesMatch(TypeVariable<?> tv1, TypeVariable<?> tv2) {
        return boundsMatch(tv1.getBounds(), tv2.getBounds());
    }

    private boolean boundsMatch(Type[] bounds1, Type[] bounds2) {
        if (bounds1.length != bounds2.length) {
            return false;
        }
        for (int i = 0; i < bounds1.length; i++) {
            if (!typesMatch(bounds1[i], bounds2[i])) {
                return false;
            }
        }
        return true;
    }
}
