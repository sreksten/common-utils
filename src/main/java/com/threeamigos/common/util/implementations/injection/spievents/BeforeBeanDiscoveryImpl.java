package com.threeamigos.common.util.implementations.injection.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import jakarta.enterprise.inject.spi.*;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import java.lang.annotation.Annotation;

/**
 * BeforeBeanDiscovery event implementation.
 * 
 * <p>Fired before the bean discovery process starts. Extensions can use this event to:
 * <ul>
 *   <li>Add qualifiers via {@link #addQualifier(Class)}</li>
 *   <li>Add scopes via {@link #addScope(Class, boolean, boolean)}</li>
 *   <li>Add stereotypes via {@link #addStereotype(Class, Annotation...)}</li>
 *   <li>Add interceptor bindings via {@link #addInterceptorBinding(Class, Annotation...)}</li>
 *   <li>Add annotated types programmatically via {@link #addAnnotatedType(AnnotatedType, String)}</li>
 * </ul>
 *
 * @see jakarta.enterprise.inject.spi.BeforeBeanDiscovery
 */
public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery {

    private final KnowledgeBase knowledgeBase;
    private final BeanManager beanManager;

    public BeforeBeanDiscoveryImpl(KnowledgeBase knowledgeBase, BeanManager beanManager) {
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
    }

    @Override
    public void addQualifier(Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Adding qualifier: " + qualifier.getSimpleName());
        knowledgeBase.addQualifier(qualifier);
    }

    @Override
    public void addQualifier(AnnotatedType<? extends Annotation> qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier AnnotatedType cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Adding qualifier from AnnotatedType: " +
                          qualifier.getJavaClass().getSimpleName());

        // Extract the qualifier class from the AnnotatedType and register it
        knowledgeBase.addQualifier(qualifier.getJavaClass());
    }

    @Override
    public void addScope(Class<? extends Annotation> scopeType, boolean normal, boolean passivating) {
        if (scopeType == null) {
            throw new IllegalArgumentException("Scope type cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Adding scope: " + scopeType.getSimpleName() +
                          " (normal=" + normal + ", passivating=" + passivating + ")");

        knowledgeBase.addScope(scopeType, normal, passivating);
    }

    @Override
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef) {
        if (stereotype == null) {
            throw new IllegalArgumentException("Stereotype cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Adding stereotype: " + stereotype.getSimpleName() +
                          " with " + (stereotypeDef != null ? stereotypeDef.length : 0) + " meta-annotation(s)");

        // Register the stereotype with its meta-annotations in the knowledge base
        knowledgeBase.addStereotype(stereotype, stereotypeDef);
    }

    @Override
    public void addInterceptorBinding(AnnotatedType<? extends Annotation> bindingType) {
        if (bindingType == null) {
            throw new IllegalArgumentException("Interceptor binding AnnotatedType cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Adding interceptor binding from AnnotatedType: " +
                          bindingType.getJavaClass().getSimpleName());

        // Extract meta-annotations from the AnnotatedType
        Annotation[] metaAnnotations = bindingType.getAnnotations().toArray(new Annotation[0]);

        // Register the interceptor binding with its meta-annotations
        knowledgeBase.addInterceptorBinding(bindingType.getJavaClass(), metaAnnotations);
    }

    @Override
    public void addInterceptorBinding(Class<? extends Annotation> bindingType, Annotation... bindingTypeDef) {
        if (bindingType == null) {
            throw new IllegalArgumentException("Interceptor binding type cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Adding interceptor binding: " + bindingType.getSimpleName() +
                          " with " + (bindingTypeDef != null ? bindingTypeDef.length : 0) + " meta-annotation(s)");

        knowledgeBase.addInterceptorBinding(bindingType, bindingTypeDef);
    }

    @Override
    public void addAnnotatedType(AnnotatedType<?> type, String id) {
        if (type == null) {
            throw new IllegalArgumentException("AnnotatedType cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Adding annotated type: " + type.getJavaClass().getName() +
                          " with ID: " + id);

        knowledgeBase.addAnnotatedType(type, id);
    }

    @Override
    public <T> AnnotatedTypeConfigurator<T> addAnnotatedType(Class<T> type, String id) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        System.out.println("[BeforeBeanDiscovery] Creating AnnotatedTypeConfigurator for: " +
                          type.getName() + " with ID: " + id);

        // Create an AnnotatedType from the class using BeanManager
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(type);

        // Return a configurator that will register the type when complete() is called
        return new AnnotatedTypeConfiguratorImpl<T>(annotatedType) {
            @Override
            public AnnotatedType<T> complete() {
                AnnotatedType<T> configured = super.complete();
                knowledgeBase.addAnnotatedType(configured, id);
                return configured;
            }
        };
    }

    @Override
    public <T extends Annotation> AnnotatedTypeConfigurator<T> configureQualifier(Class<T> qualifier) {
        // TODO: Return configurator for configuring qualifier
        System.out.println("BeforeBeanDiscovery: configureQualifier(" + qualifier.getName() + ")");
        throw new UnsupportedOperationException("AnnotatedTypeConfigurator not yet implemented");
    }

    @Override
    public <T extends Annotation> AnnotatedTypeConfigurator<T> configureInterceptorBinding(Class<T> bindingType) {
        // TODO: Return configurator for configuring interceptor binding
        System.out.println("BeforeBeanDiscovery: configureInterceptorBinding(" + bindingType.getName() + ")");
        throw new UnsupportedOperationException("AnnotatedTypeConfigurator not yet implemented");
    }
}
