package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JSR330Validator {

    private final KnowledgeBase knowledgeBase;
    private boolean strict;

    JSR330Validator(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = Objects.requireNonNull(knowledgeBase, "knowledgeBase cannot be null");
        strict = false;
    }

    /**
     * Enforces strict JSR330 specifications: no more than one qualifier should annotate a single field or parameter.
     *
     * @param strict if true, enforces strict JSR330 specifications
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isStrict() {
        return strict;
    }

    <T> boolean isValid(Class<T> clazz) {
        Objects.requireNonNull(clazz, "Class cannot be null");

        boolean valid = true;

        boolean hasInjectedElements = false;

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                hasInjectedElements = true;

                if (Modifier.isFinal(field.getModifiers())) {
                    knowledgeBase.addError(fmtField(field) + ": it is a final field, thus cannot be used as an injection point");
                    valid = false;
                }

                if (strict && Modifier.isStatic(field.getModifiers())) {
                    knowledgeBase.addError(fmtField(field) + ": it is a static field, thus cannot be used as an injection point");
                    valid = false;
                }

                try {
                    checkClassValidity(field.getGenericType());
                } catch (IllegalArgumentException e) {
                    knowledgeBase.addError(fmtField(field) + ": " + e.getMessage());
                    valid = false;
                } catch (DefinitionException e) {
                    knowledgeBase.addDefinitionError(fmtField(field) + ": " + e.getMessage());
                    valid = false;
                }

                try {
                    checkAnnotations(field.getAnnotations());
                } catch (DefinitionException e) {
                    knowledgeBase.addDefinitionError(fmtField(field) + ": " + e.getMessage());
                    valid = false;
                }
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Inject.class)) {
                hasInjectedElements = true;

                if (strict && clazz.isInterface() && !Modifier.isAbstract(method.getModifiers())) {
                    knowledgeBase.addWarning("@Inject on interface default method is not portable");
                }

                if (strict && Modifier.isPrivate(method.getModifiers())) {
                    knowledgeBase.addWarning(fmtMethod(method) + ": private method injection is implementation-dependent");
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    knowledgeBase.addInjectionError(fmtMethod(method) + ": cannot perform injection on an abstract method");
                    valid = false;
                }

                if (strict && Modifier.isStatic(method.getModifiers())) {
                    knowledgeBase.addInjectionError(fmtMethod(method) + ": cannot perform injection on a static method");
                    valid = false;
                }

                if (method.getTypeParameters().length > 0) {
                    knowledgeBase.addInjectionError(fmtMethod(method) + ": cannot perform injection on a generic method");
                    valid = false;
                }

                if (!hasValidParameters(method.getParameters())) {
                    valid = false;
                }

            } else {
                // Check if this method overrides an @Inject method from any superclass in the hierarchy
                checkInjectMethodOverride(clazz, method);
            }
        }

        if (hasInjectedElements) {
            @SuppressWarnings("unchecked")
            Constructor<T> constructor = (Constructor<T>) findConstructor(clazz);
            if (constructor != null) {
                knowledgeBase.addConstructor(clazz, constructor);
            } else {
                valid = false;
            }
        }

        return valid;
    }

    private String fmtField(Field field) {
        return "Field " + field.getName() + " of class " + field.getDeclaringClass().getName();
    }

    private String fmtMethod(Method method) {
        return "Method " + method.getName() + " of class " + method.getDeclaringClass().getName();
    }

    private String fmtParameter(Parameter parameter) {
        return "Parameter " + parameter.getName() + " of method " + parameter.getDeclaringExecutable().getName() + " of class " + parameter.getDeclaringExecutable().getDeclaringClass().getName();
    }

    /**
     * Validates that a type is suitable for dependency injection according to JSR-330 specifications
     * and Java language constraints. This method enforces type safety and prevents injection of
     * problematic types that cannot be properly instantiated or managed by the dependency injection
     * container.
     *
     * <p><b>Validation Rules (JSR-330 and Java Constraints):</b>
     * <ol>
     *   <li><b>Enums:</b> Cannot be injected because enums have predefined instances and
     *       cannot be instantiated via constructors. Use {@code @Inject} on fields within
     *       the enum instead.</li>
     *   <li><b>Primitives:</b> Cannot be injected because primitives have no constructors and
     *       cannot be null. Use wrapper classes (Integer, Boolean, etc.) instead.</li>
     *   <li><b>Synthetic Classes:</b> Cannot be injected because these are compiler-generated
     *       internal classes (e.g., lambda implementations, bridges) not intended for direct use.</li>
     *   <li><b>Local Classes:</b> Cannot be injected because local classes (defined within methods)
     *       may capture local variables and have complex scoping rules.</li>
     *   <li><b>Anonymous Classes:</b> Cannot be injected because anonymous classes have no
     *       constructors that can be annotated with {@code @Inject} and are typically
     *       single-use instances.</li>
     *   <li><b>Non-static Inner Classes:</b> Cannot be injected because non-static inner classes
     *       require an enclosing instance, which the injector cannot provide. Use static
     *       nested classes instead.</li>
     * </ol>
     *
     * <p><b>Recursive Validation:</b>
     * For parameterized types (generics), this method recursively validates all type arguments.
     * For example, {@code List<MyEnum>} will be rejected because {@code MyEnum} is not injectable,
     * even though {@code List} itself would be valid. This ensures type safety throughout the
     * entire type hierarchy.
     *
     * <p><b>Examples of Invalid Types:</b>
     * <pre>{@code
     * // Enums - INVALID
     * @Inject MyEnum myEnum; // Rejected
     *
     * // Primitives - INVALID
     * @Inject int count; // Rejected (use Integer instead)
     *
     * // Non-static inner classes - INVALID
     * class Outer {
     *     class Inner { } // Rejected (make it static)
     * }
     *
     * // Parameterized with invalid type argument - INVALID
     * @Inject List<MyEnum> enums; // Rejected (MyEnum is not injectable)
     * }</pre>
     *
     * @param type the type to validate (can be a Class or ParameterizedType)
     * @throws IllegalArgumentException if the type violates any injection constraints
     * @see RawTypeExtractor#getRawType(Type)
     * @see jakarta.inject.Inject
     */
    void checkClassValidity(Type type) {
        if (type instanceof WildcardType) {
            throw new DefinitionException("Injection point cannot contain a wildcard (" + type.getTypeName() + ")");
        }

        // Check for type variables (e.g., T, E, K, V)
        if (type instanceof TypeVariable) {
            throw new DefinitionException("Injection point cannot be a type variable (" + type.getTypeName() + ")");
        }

        Class<?> clazz = RawTypeExtractor.getRawType(type);

        if (clazz.isArray()) {
            throw new IllegalArgumentException("Cannot inject arrays directly - use Provider<T>[] or List<T> instead");
        }
        // Basic JSR-330 and Java constraints
        if (clazz.isEnum()) {
            throw new IllegalArgumentException("Cannot inject an enum");
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("Cannot inject a primitive");
        }
        if (clazz.isSynthetic()) {
            throw new IllegalArgumentException("Cannot inject a synthetic class");
        }
        if (clazz.isLocalClass()) {
            throw new IllegalArgumentException("Cannot inject a local class");
        }
        if (clazz.isAnonymousClass()) {
            throw new IllegalArgumentException("Cannot inject an anonymous class");
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new IllegalArgumentException("Cannot inject a non-static inner class");
        }

        // Recursive validation for Parameterized Types (Generics)
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

    void checkAnnotations(Annotation[] annotations) {
        if (strict) {
            List<Annotation> qualifiers = Arrays.stream(annotations)
                    .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
                    .collect(Collectors.toList());
            if (qualifiers.size() > 1) {
                String qualifierNames = qualifiers.stream()
                        .map(q -> "@" + q.annotationType().getSimpleName())
                        .collect(Collectors.joining(", "));
                throw new DefinitionException("more than one qualifier (" + qualifierNames + ") found. JSR-330 allows at most one qualifier per injection point.");
            }
        }
    }

    Constructor<?> findConstructor(Class<?> clazz) {
        List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (constructors.size() > 1) {
            knowledgeBase.addInjectionError(clazz.getName() + ": more than one constructor annotated with @Inject");
            return null;
        } else if (constructors.size() == 1) {
            Constructor<?> candidateConstructor = constructors.get(0);
            if (hasValidParameters(candidateConstructor.getParameters())) {
                return candidateConstructor;
            } else {
                return null;
            }
        } else {
            constructors = Arrays.stream(clazz.getDeclaredConstructors())
                    .filter(c -> c.getParameterCount() == 0)
                    .collect(Collectors.toList());
            if (constructors.isEmpty()) {
                knowledgeBase.addInjectionError(clazz.getName() + ": No empty constructor or a constructor annotated with @Inject");
                return null;
            } else {
                Constructor<?> noArgConstructor = constructors.get(0);
                if (Modifier.isPrivate(noArgConstructor.getModifiers())) {
                    knowledgeBase.addWarning(clazz.getName() + ": no-arg constructor is private - may require reflection workarounds");
                }
                return noArgConstructor;
            }
        }
    }

    boolean hasValidParameters(Parameter[] parameters) {
        boolean valid = true;
        for (Parameter parameter : parameters) {
            try {
                checkClassValidity(parameter.getParameterizedType());
            } catch (IllegalArgumentException e) {
                knowledgeBase.addError(fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            }

            try {
                checkAnnotations(parameter.getAnnotations());
            } catch (DefinitionException e) {
                knowledgeBase.addDefinitionError(fmtParameter(parameter) + ": " + e.getMessage());
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Checks if a method in a subclass overrides an @Inject-annotated method from any superclass
     * in the inheritance hierarchy. According to JSR-330, if a superclass method has @Inject
     * but the overriding subclass method does not, the method will NOT be injected.
     *
     * <p>This method traverses the entire class hierarchy from the given class up to Object,
     * checking each superclass for a matching method signature. If found and annotated with
     * @Inject, a warning is logged.
     *
     * <p><b>Example scenario:</b>
     * <pre>{@code
     * class Parent {
     *     @Inject
     *     public void configure(Service service) { }
     * }
     *
     * class Child extends Parent {
     *     @Override
     *     public void configure(Service service) { }  // ← Missing @Inject - will NOT be injected!
     * }
     * }</pre>
     *
     * @param clazz the class containing the method to check
     * @param method the method to check for @Inject override violations
     */
    private void checkInjectMethodOverride(Class<?> clazz, Method method) {
        Class<?> currentClass = clazz.getSuperclass();

        // Traverse the entire inheritance hierarchy up to Object
        while (currentClass != null && currentClass != Object.class) {
            try {
                // Look for a method with the same signature in the current superclass
                Method superMethod = currentClass.getDeclaredMethod(method.getName(), method.getParameterTypes());

                // If the superclass method has @Inject, warn about the override
                if (superMethod.isAnnotationPresent(Inject.class)) {
                    knowledgeBase.addWarning(fmtMethod(method) +
                        ": overrides @Inject method from " + currentClass.getName() +
                        " but is not annotated - injection will NOT occur");
                    // Found the @Inject annotation in hierarchy, no need to check further up
                    return;
                }
            } catch (NoSuchMethodException e) {
                // Method not declared in this superclass, continue to next level
            }

            // Move up the hierarchy
            currentClass = currentClass.getSuperclass();
        }
    }

}
