package com.threeamigos.common.util.implementations.injection.knowledgebase;

import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BinaryOperator;

final class KnowledgeBaseDiscoveryStore {

    private final Set<Class<?>> excludedClasses = new HashSet<>();
    private final Set<Class<?>> classes = ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, BeanArchiveMode> classArchiveModes = new ConcurrentHashMap<>();
    private final Map<Class<?>, AnnotatedType<?>> annotatedTypeOverrides = new ConcurrentHashMap<>();
    private final Map<Class<?>, Constructor<?>> constructorsMap = new ConcurrentHashMap<>();
    private final Collection<BeansXml> beansXmlConfigurations = new ConcurrentLinkedQueue<>();
    private final Set<Class<?>> vetoedTypes = ConcurrentHashMap.newKeySet();
    private volatile boolean implicitBeanArchiveScanningEnabled = true;

    void exclude(Class<?>... excludedTypes) {
        excludedClasses.addAll(Arrays.asList(excludedTypes));
    }

    Collection<Class<?>> getExcludedClasses() {
        return excludedClasses;
    }

    boolean isExcluded(Class<?> clazz) {
        return excludedClasses.contains(clazz);
    }

    void addDiscoveredClass(Class<?> clazz) {
        if (isExcluded(clazz)) {
            return;
        }
        classes.add(clazz);
    }

    void addProgrammaticClass(Class<?> clazz) {
        classes.add(clazz);
    }

    Collection<Class<?>> getClasses() {
        return classes;
    }

    boolean containsClass(Class<?> clazz) {
        return classes.contains(clazz);
    }

    void setBeanArchiveMode(Class<?> clazz, BeanArchiveMode mode) {
        classArchiveModes.put(clazz, mode);
    }

    void mergeBeanArchiveMode(Class<?> clazz,
                              BeanArchiveMode incomingMode,
                              BinaryOperator<BeanArchiveMode> mergeFunction) {
        classArchiveModes.compute(clazz, (ignored, currentMode) -> mergeFunction.apply(currentMode, incomingMode));
    }

    BeanArchiveMode getBeanArchiveModeOrDefault(Class<?> clazz) {
        return classArchiveModes.getOrDefault(clazz, BeanArchiveMode.IMPLICIT);
    }

    void setAnnotatedTypeOverride(Class<?> clazz, AnnotatedType<?> annotatedType) {
        annotatedTypeOverrides.put(clazz, annotatedType);
    }

    AnnotatedType<?> getAnnotatedTypeOverride(Class<?> clazz) {
        return annotatedTypeOverrides.get(clazz);
    }

    void setConstructor(Class<?> clazz, Constructor<?> constructor) {
        constructorsMap.put(clazz, constructor);
    }

    Constructor<?> getConstructor(Class<?> clazz) {
        return constructorsMap.get(clazz);
    }

    void removeDiscoveredClass(Class<?> clazz) {
        classes.remove(clazz);
        classArchiveModes.remove(clazz);
        annotatedTypeOverrides.remove(clazz);
        constructorsMap.remove(clazz);
        vetoedTypes.remove(clazz);
    }

    boolean hasNotBeansXmlConfigurations() {
        return beansXmlConfigurations.isEmpty();
    }

    void addBeansXmlConfiguration(BeansXml beansXml) {
        beansXmlConfigurations.add(beansXml);
    }

    Collection<BeansXml> getBeansXmlConfigurations() {
        return beansXmlConfigurations;
    }

    void vetoType(Class<?> clazz) {
        vetoedTypes.add(clazz);
    }

    boolean isTypeVetoed(Class<?> clazz) {
        return vetoedTypes.contains(clazz);
    }

    Set<Class<?>> getVetoedTypes() {
        return vetoedTypes;
    }

    boolean isImplicitBeanArchiveScanningEnabled() {
        return implicitBeanArchiveScanningEnabled;
    }

    void setImplicitBeanArchiveScanningEnabled(boolean enabled) {
        this.implicitBeanArchiveScanningEnabled = enabled;
    }

    void clear() {
        excludedClasses.clear();
        classes.clear();
        classArchiveModes.clear();
        annotatedTypeOverrides.clear();
        constructorsMap.clear();
        beansXmlConfigurations.clear();
        vetoedTypes.clear();
        implicitBeanArchiveScanningEnabled = true;
    }
}
