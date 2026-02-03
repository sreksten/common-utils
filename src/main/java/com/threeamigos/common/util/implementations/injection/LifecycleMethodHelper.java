package com.threeamigos.common.util.implementations.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class LifecycleMethodHelper {

    static void invokeLifecycleMethod(Object instance, Class<? extends Annotation> annotation) throws InvocationTargetException, IllegalAccessException {
        // Collect all classes in the hierarchy, from parent to child
        List<Class<?>> hierarchy = buildHierarchy(instance);

        // Invoke @PreDestroy methods from parent to child
        for (Class<?> clazz : hierarchy) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    method.setAccessible(true);
                    method.invoke(instance);
                }
            }
        }
    }

    static List<Class<?>> buildHierarchy(Object instance) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = instance.getClass();
        while (current != Object.class) {
            hierarchy.add(0, current); // Add as first, so we process parent classes first
            current = current.getSuperclass();
        }
        return hierarchy;
    }
}
