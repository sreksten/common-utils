package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.SyntheticBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanBuilder;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanDisposer;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.Type;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class BceSyntheticBeanBuilderImpl<T> implements SyntheticBeanBuilder<T> {

    private final KnowledgeBase knowledgeBase;
    private final BeanManagerImpl beanManager;
    private final BceInvokerRegistry invokerRegistry;
    private final Class<T> implementationClass;

    private final Set<java.lang.reflect.Type> types = new LinkedHashSet<java.lang.reflect.Type>();
    private final Set<Annotation> qualifiers = new LinkedHashSet<Annotation>();
    private final Set<Class<? extends Annotation>> stereotypes = new LinkedHashSet<Class<? extends Annotation>>();
    private final Map<String, Object> params = new LinkedHashMap<String, Object>();
    private Class<? extends Annotation> scope = Dependent.class;
    private boolean alternative;
    private Integer priority;
    private String name;
    private Class<? extends SyntheticBeanCreator<T>> creatorClass;
    private Class<? extends SyntheticBeanDisposer<T>> disposerClass;

    BceSyntheticBeanBuilderImpl(KnowledgeBase knowledgeBase,
                                BeanManagerImpl beanManager,
                                BceInvokerRegistry invokerRegistry,
                                Class<T> implementationClass) {
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
        this.invokerRegistry = invokerRegistry;
        this.implementationClass = implementationClass;
    }

    @Override
    public SyntheticBeanBuilder<T> type(Class<?> type) {
        if (type != null) {
            this.types.add(type);
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> type(ClassInfo type) {
        if (type != null) {
            this.types.add(BceMetadata.unwrapClassInfo(type));
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> type(Type type) {
        if (type != null) {
            this.types.add(BceMetadata.unwrapType(type));
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> qualifier(Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            return this;
        }
        try {
            Annotation annotation = qualifier.getDeclaredConstructor().newInstance();
            this.qualifiers.add(annotation);
            return this;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate qualifier " + qualifier.getName() +
                ". Use qualifier(Annotation) or qualifier(AnnotationInfo).", e);
        }
    }

    @Override
    public SyntheticBeanBuilder<T> qualifier(AnnotationInfo qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(BceMetadata.unwrapAnnotationInfo(qualifier));
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> qualifier(Annotation qualifier) {
        if (qualifier != null) {
            this.qualifiers.add(qualifier);
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> scope(Class<? extends Annotation> scope) {
        if (scope != null) {
            this.scope = scope;
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> alternative(boolean alternative) {
        this.alternative = alternative;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> stereotype(Class<? extends Annotation> stereotype) {
        if (stereotype != null) {
            this.stereotypes.add(stereotype);
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> stereotype(ClassInfo stereotype) {
        if (stereotype != null) {
            Class<?> stereotypeClass = BceMetadata.unwrapClassInfo(stereotype);
            if (!Annotation.class.isAssignableFrom(stereotypeClass)) {
                throw new IllegalArgumentException("Stereotype ClassInfo does not represent annotation type: " +
                    stereotypeClass.getName());
            }
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> annType = (Class<? extends Annotation>) stereotypeClass;
            this.stereotypes.add(annType);
        }
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, boolean value) {
        return withParamInternal(name, Boolean.valueOf(value));
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, boolean[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, int value) {
        return withParamInternal(name, Integer.valueOf(value));
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, int[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, long value) {
        return withParamInternal(name, Long.valueOf(value));
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, long[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, double value) {
        return withParamInternal(name, Double.valueOf(value));
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, double[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, String value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, String[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, Enum<?> value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, Enum<?>[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, Class<?> value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, ClassInfo value) {
        return withParamInternal(name, value != null ? BceMetadata.unwrapClassInfo(value) : null);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, Class<?>[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, ClassInfo[] value) {
        if (value == null) {
            return withParamInternal(name, null);
        }
        Class<?>[] converted = new Class<?>[value.length];
        for (int i = 0; i < value.length; i++) {
            converted[i] = value[i] != null ? BceMetadata.unwrapClassInfo(value[i]) : null;
        }
        return withParamInternal(name, converted);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, AnnotationInfo value) {
        return withParamInternal(name, value != null ? BceMetadata.unwrapAnnotationInfo(value) : null);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, Annotation value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, AnnotationInfo[] value) {
        if (value == null) {
            return withParamInternal(name, null);
        }
        Annotation[] converted = new Annotation[value.length];
        for (int i = 0; i < value.length; i++) {
            converted[i] = value[i] != null ? BceMetadata.unwrapAnnotationInfo(value[i]) : null;
        }
        return withParamInternal(name, converted);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, Annotation[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, InvokerInfo value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> withParam(String name, InvokerInfo[] value) {
        return withParamInternal(name, value);
    }

    @Override
    public SyntheticBeanBuilder<T> createWith(Class<? extends SyntheticBeanCreator<T>> creatorClass) {
        this.creatorClass = creatorClass;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> disposeWith(Class<? extends SyntheticBeanDisposer<T>> disposerClass) {
        this.disposerClass = disposerClass;
        return this;
    }

    private SyntheticBeanBuilder<T> withParamInternal(String name, Object value) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter name must not be blank");
        }
        this.params.put(name, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    void complete() {
        if (creatorClass == null) {
            throw new IllegalStateException("Synthetic bean creator is required via createWith()");
        }

        final Map<String, Object> frozenParams = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(params));
        final Class<? extends SyntheticBeanCreator<T>> frozenCreatorClass = creatorClass;
        final Class<? extends SyntheticBeanDisposer<T>> frozenDisposerClass = disposerClass;

        Function<CreationalContext<T>, T> createCallback = ctx -> {
            try {
                SyntheticBeanCreator<T> creator = frozenCreatorClass.getDeclaredConstructor().newInstance();
                return creator.create(beanManager.createInstance(), new BceParameters(frozenParams, invokerRegistry));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate synthetic bean creator " +
                    frozenCreatorClass.getName(), e);
            }
        };

        BiConsumer<T, CreationalContext<T>> destroyCallback = null;
        if (frozenDisposerClass != null) {
            destroyCallback = (instance, ctx) -> {
                try {
                    SyntheticBeanDisposer<T> disposer = frozenDisposerClass.getDeclaredConstructor().newInstance();
                    disposer.dispose(instance, beanManager.createInstance(),
                        new BceParameters(frozenParams, invokerRegistry));
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate synthetic bean disposer " +
                        frozenDisposerClass.getName(), e);
                }
            };
        }

        Set<java.lang.reflect.Type> beanTypes = new LinkedHashSet<java.lang.reflect.Type>(types);
        if (beanTypes.isEmpty()) {
            beanTypes.add(implementationClass);
            beanTypes.add(Object.class);
        }

        Set<Annotation> beanQualifiers = new LinkedHashSet<Annotation>(qualifiers);
        if (beanQualifiers.isEmpty()) {
            beanQualifiers.add(Default.Literal.INSTANCE);
        }

        SyntheticBean<T> bean = new SyntheticBean<T>(
            implementationClass,
            beanTypes,
            beanQualifiers,
            scope,
            name,
            stereotypes,
            alternative,
            priority,
            createCallback,
            destroyCallback,
            Collections.emptySet()
        );
        knowledgeBase.addBean(bean);
    }
}
