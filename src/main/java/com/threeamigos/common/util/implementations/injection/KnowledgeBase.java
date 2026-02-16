package com.threeamigos.common.util.implementations.injection;

import jakarta.enterprise.inject.spi.Bean;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

class KnowledgeBase {

    private final Collection<Class<?>> classes = new ConcurrentLinkedQueue<>();
    private final Collection<Bean<?>> beans = new ConcurrentLinkedQueue<>();

    private final Map<Class<?>, Constructor<?>> constructorsMap = new ConcurrentHashMap<>();

    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> definitionErrors = new ArrayList<>();
    private final List<String> injectionErrors = new ArrayList<>();

    public void add(Class<?> clazz) {
        classes.add(clazz);
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }

    void addBean(Bean<?> bean) {
        beans.add(bean);
    }

    Collection<Bean<?>> getBeans() {
        return beans;
    }

    <T> void addConstructor(Class<T> clazz, Constructor<T> constructor) {
        constructorsMap.put(clazz, constructor);
    }

    @SuppressWarnings("unchecked")
    <T> Constructor<T> getConstructor(Class<T> clazz) {
        return (Constructor<T>)constructorsMap.get(clazz);
    }

    void addWarning(String warning) {
        warnings.add(warning);
    }

    List<String> getWarnings() {
        return warnings;
    }

    void addError(String error) {
        errors.add(error);
    }

    List<String> getErrors() {
        return errors;
    }

    void addDefinitionError(String error) {
        definitionErrors.add(error);
    }

    List<String> getDefinitionErrors() {
        return definitionErrors;
    }

    void addInjectionError(String error) {
        injectionErrors.add(error);
    }

    List<String> getInjectionErrors() {
        return injectionErrors;
    }

    /**
     * Checks if there are any critical errors that would prevent application startup.
     * This includes definition errors, injection errors, and general errors.
     *
     * @return true if there are any errors that should stop the application
     */
    boolean hasErrors() {
        return !definitionErrors.isEmpty() || !injectionErrors.isEmpty() || !errors.isEmpty();
    }

    /**
     * Returns all beans that have validation errors.
     * These beans were discovered but failed validation.
     * The application should only fail if these beans are actually needed for injection.
     *
     * @return collection of beans with validation errors
     */
    Collection<Bean<?>> getBeansWithValidationErrors() {
        List<Bean<?>> beansWithErrors = new ArrayList<>();
        for (Bean<?> bean : beans) {
            if (bean instanceof BeanImpl && ((BeanImpl<?>) bean).hasValidationErrors()) {
                beansWithErrors.add(bean);
            }
        }
        return beansWithErrors;
    }

    /**
     * Returns all beans that are valid (no validation errors).
     * These beans are candidates for dependency injection.
     *
     * @return collection of valid beans
     */
    Collection<Bean<?>> getValidBeans() {
        List<Bean<?>> validBeans = new ArrayList<>();
        for (Bean<?> bean : beans) {
            if (!(bean instanceof BeanImpl) || !((BeanImpl<?>) bean).hasValidationErrors()) {
                validBeans.add(bean);
            }
        }
        return validBeans;
    }
}