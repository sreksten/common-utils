package com.threeamigos.common.util.implementations.injection.bce;

import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.invoke.Invoker;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters implementation that materializes InvokerInfo tokens into runtime Invokers.
 */
public class BceParameters implements Parameters {

    private final Map<String, Object> values;
    private final BceInvokerRegistry invokerRegistry;

    public BceParameters(Map<String, Object> values, BceInvokerRegistry invokerRegistry) {
        this.values = values != null ? new HashMap<String, Object>(values) : new HashMap<String, Object>();
        this.invokerRegistry = invokerRegistry;
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        if (!values.containsKey(name)) {
            throw new IllegalArgumentException("Missing parameter '" + name + "'");
        }
        Object raw = values.get(name);
        return convert(name, type, raw);
    }

    @Override
    public <T> T get(String name, Class<T> type, T defaultValue) {
        if (!values.containsKey(name)) {
            return defaultValue;
        }
        Object raw = values.get(name);
        return convert(name, type, raw);
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(String name, Class<T> type, Object raw) {
        if (raw == null) {
            return null;
        }

        Object converted = raw;
        if (raw instanceof InvokerInfo) {
            converted = materializeInvoker((InvokerInfo) raw);
        } else if (raw instanceof InvokerInfo[]) {
            InvokerInfo[] infos = (InvokerInfo[]) raw;
            Invoker[] invokers = new Invoker[infos.length];
            for (int i = 0; i < infos.length; i++) {
                invokers[i] = materializeInvoker(infos[i]);
            }
            converted = invokers;
        } else if (raw instanceof jakarta.enterprise.lang.model.declarations.ClassInfo) {
            converted = BceMetadata.unwrapClassInfo((jakarta.enterprise.lang.model.declarations.ClassInfo) raw);
        } else if (raw instanceof jakarta.enterprise.lang.model.declarations.ClassInfo[]) {
            jakarta.enterprise.lang.model.declarations.ClassInfo[] infos =
                (jakarta.enterprise.lang.model.declarations.ClassInfo[]) raw;
            Class<?>[] classes = new Class<?>[infos.length];
            for (int i = 0; i < infos.length; i++) {
                classes[i] = infos[i] != null ? BceMetadata.unwrapClassInfo(infos[i]) : null;
            }
            converted = classes;
        } else if (raw instanceof jakarta.enterprise.lang.model.AnnotationInfo) {
            converted = BceMetadata.unwrapAnnotationInfo((jakarta.enterprise.lang.model.AnnotationInfo) raw);
        } else if (raw instanceof jakarta.enterprise.lang.model.AnnotationInfo[]) {
            jakarta.enterprise.lang.model.AnnotationInfo[] infos =
                (jakarta.enterprise.lang.model.AnnotationInfo[]) raw;
            java.lang.annotation.Annotation[] annotations = new java.lang.annotation.Annotation[infos.length];
            for (int i = 0; i < infos.length; i++) {
                annotations[i] = infos[i] != null ? BceMetadata.unwrapAnnotationInfo(infos[i]) : null;
            }
            converted = annotations;
        } else if (raw instanceof jakarta.enterprise.lang.model.types.Type) {
            converted = BceMetadata.unwrapType((jakarta.enterprise.lang.model.types.Type) raw);
        }

        if (converted != null && !type.isInstance(converted)) {
            if (converted instanceof java.lang.annotation.Annotation[] &&
                type.isArray() &&
                java.lang.annotation.Annotation.class.isAssignableFrom(type.getComponentType())) {
                java.lang.annotation.Annotation[] source = (java.lang.annotation.Annotation[]) converted;
                Object typedArray = java.lang.reflect.Array.newInstance(type.getComponentType(), source.length);
                for (int i = 0; i < source.length; i++) {
                    java.lang.reflect.Array.set(typedArray, i, source[i]);
                }
                converted = typedArray;
            }
        }

        if (converted != null && !type.isInstance(converted)) {
            throw new IllegalArgumentException("Parameter '" + name + "' is not of expected type " +
                type.getName() + " (actual: " + converted.getClass().getName() + ")");
        }
        return (T) converted;
    }

    @SuppressWarnings("unchecked")
    private Invoker<Object, Object> materializeInvoker(InvokerInfo info) {
        if (invokerRegistry == null) {
            throw new IllegalStateException("No invoker registry available to materialize InvokerInfo");
        }
        return (Invoker<Object, Object>) invokerRegistry.resolve(info);
    }
}
