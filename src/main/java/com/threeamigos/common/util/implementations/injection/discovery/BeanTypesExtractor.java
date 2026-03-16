package com.threeamigos.common.util.implementations.injection.discovery;

import com.threeamigos.common.util.implementations.injection.util.TypeClosureHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.getTypedAnnotation;
import static com.threeamigos.common.util.implementations.injection.AnnotationsEnum.hasTypedAnnotation;

/**
 * Extracts bean type sets for managed beans and producers.
 *
 * <p>Designed as a stateless component: create one instance per validation flow
 * or reuse safely across threads.
 */
public final class BeanTypesExtractor {

    /**
     * Extracts resulting bean types for a managed bean class.
     *
     * <p>The result is the unrestricted managed-bean type set optionally restricted by
     * {@code @Typed}, with illegal bean types removed.
     */
    public ExtractionResult extractManagedBeanTypes(Class<?> beanClass) {
        Objects.requireNonNull(beanClass, "beanClass cannot be null");

        List<String> definitionErrors = new ArrayList<>();
        Set<Type> unrestrictedTypes = computeManagedUnrestrictedTypes(beanClass, definitionErrors);
        Set<Type> legalTypes = keepLegalBeanTypes(unrestrictedTypes);
        return new ExtractionResult(legalTypes, definitionErrors);
    }

    /**
     * Extracts resulting bean types for a producer method/field type.
     *
     * <p>The result is the unrestricted producer type set with illegal bean types removed.
     */
    public ExtractionResult extractProducerBeanTypes(Type producerType) {
        Objects.requireNonNull(producerType, "producerType cannot be null");

        Set<Type> unrestrictedTypes = TypeClosureHelper.extractTypesFromType(producerType);
        Set<Type> legalTypes = keepLegalBeanTypes(unrestrictedTypes);
        return new ExtractionResult(legalTypes, Collections.emptyList());
    }

    private Set<Type> computeManagedUnrestrictedTypes(Class<?> beanClass, List<String> definitionErrors) {
        if (hasTypedAnnotation(beanClass)) {
            Annotation typedAnnotation = getTypedAnnotation(beanClass);
            if (typedAnnotation != null) {
                return computeTypedBeanTypes(beanClass, typedAnnotation, definitionErrors);
            }
        }

        return TypeClosureHelper.extractTypesFromClass(beanClass);
    }

    private Set<Type> computeTypedBeanTypes(Class<?> beanClass, Annotation typedAnnotation, List<String> definitionErrors) {
        Set<Type> types = new LinkedHashSet<>();

        try {
            Method valueMethod = typedAnnotation.annotationType().getMethod("value");
            Class<?>[] typedClasses = (Class<?>[]) valueMethod.invoke(typedAnnotation);

            if (typedClasses.length == 0) {
                types.add(Object.class);
                return types;
            }

            for (Class<?> typedClass : typedClasses) {
                if (!typedClass.isAssignableFrom(beanClass)) {
                    definitionErrors.add("@Typed specifies type " + typedClass.getName()
                            + " which is not a type of bean class " + beanClass.getName());
                    continue;
                }
                types.add(typedClass);
            }
            types.add(Object.class);
            return types;
        } catch (ReflectiveOperationException | ClassCastException e) {
            definitionErrors.add("Failed to extract @Typed annotation values: " + e.getMessage());
            return types;
        }
    }

    private Set<Type> keepLegalBeanTypes(Set<Type> candidateTypes) {
        Set<Type> legalTypes = new LinkedHashSet<>();
        for (Type candidate : candidateTypes) {
            if (isLegalBeanType(candidate)) {
                legalTypes.add(candidate);
            }
        }
        return legalTypes;
    }

    private boolean isLegalBeanType(Type type) {
        if (type instanceof TypeVariable) {
            return false;
        }
        if (type instanceof WildcardType) {
            return false;
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return isLegalBeanType(genericArrayType.getGenericComponentType());
        }
        if (type instanceof Class) {
            Class<?> klass = (Class<?>) type;
            if (klass.isArray()) {
                return isLegalBeanType(klass.getComponentType());
            }
            return true;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                if (typeArgument instanceof WildcardType) {
                    return false;
                }
                if (typeArgument instanceof GenericArrayType) {
                    if (!isLegalBeanType(((GenericArrayType) typeArgument).getGenericComponentType())) {
                        return false;
                    }
                    continue;
                }
                if (typeArgument instanceof Class && ((Class<?>) typeArgument).isArray()) {
                    if (!isLegalBeanType(((Class<?>) typeArgument).getComponentType())) {
                        return false;
                    }
                    continue;
                }
                if (typeArgument instanceof ParameterizedType) {
                    if (!isLegalBeanType(typeArgument)) {
                        return false;
                    }
                    continue;
                }
                if (typeArgument instanceof TypeVariable) {
                    continue;
                }
                if (!(typeArgument instanceof Class)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Immutable extraction result.
     */
    public static final class ExtractionResult {
        private final Set<Type> types;
        private final List<String> definitionErrors;

        public ExtractionResult(Set<Type> types, List<String> definitionErrors) {
            this.types = Collections.unmodifiableSet(new LinkedHashSet<>(types));
            this.definitionErrors = Collections.unmodifiableList(new ArrayList<>(definitionErrors));
        }

        public Set<Type> getTypes() {
            return types;
        }

        public List<String> getDefinitionErrors() {
            return definitionErrors;
        }

        public boolean hasDefinitionErrors() {
            return !definitionErrors.isEmpty();
        }
    }
}
