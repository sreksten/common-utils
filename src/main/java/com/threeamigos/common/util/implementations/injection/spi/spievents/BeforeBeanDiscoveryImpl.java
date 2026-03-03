package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.annotation.Nonnull;
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

    private final MessageHandler messageHandler;
    private final KnowledgeBase knowledgeBase;
    private final BeanManager beanManager;

    public BeforeBeanDiscoveryImpl(MessageHandler messageHandler, KnowledgeBase knowledgeBase, BeanManager beanManager) {
        this.messageHandler = messageHandler;
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
    }

    @Override
    public void addAnnotatedType(AnnotatedType<?> type, String id) {
        if (type == null) {
            throw new IllegalArgumentException("AnnotatedType cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Adding annotated type: " + type.getJavaClass().getName() +
                " with ID: " + id);

        knowledgeBase.addAnnotatedType(type, id);
    }

    /**
     * Return a configurator that will update the annotated type's metadata when complete() is called.
     * This allows extensions to add/modify annotations on the annotated type
     * (e.g., add @Nonbinding to specific members, add meta-annotations)
     * @param type class of the annotated type
     * @param id unique identifier for the annotated type
     * @return an AnnotatedTypeConfigurator that can be used to configure the annotated type
     * @param <T> type of the annotated type
     */
    @Override
    public <T> AnnotatedTypeConfigurator<T> addAnnotatedType(Class<T> type, String id) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Creating AnnotatedTypeConfigurator for: " +
                type.getName() + " with ID: " + id);

        // Create an AnnotatedType from the class using BeanManager
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(type);

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
    public void addQualifier(Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Adding qualifier: " + qualifier.getSimpleName());
        knowledgeBase.addQualifier(qualifier);
    }

    @Override
    public void addQualifier(AnnotatedType<? extends Annotation> qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier AnnotatedType cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Adding qualifier from AnnotatedType: " +
                qualifier.getJavaClass().getSimpleName());

        // Extract the qualifier class from the AnnotatedType and register it
        knowledgeBase.addQualifier(qualifier.getJavaClass());
    }

    /**
     * Return a configurator that will update the qualifier's metadata when complete() is called.
     * This allows extensions to add/modify annotations on the qualifier annotation type
     * (e.g., add @Nonbinding to specific members, add meta-annotations)
     * @param qualifier class of the qualifier annotation
     * @return an AnnotatedTypeConfigurator that can be used to configure the qualifier
     * @param <T> type of the qualifier annotation
     */
    @Override
    public <T extends Annotation> AnnotatedTypeConfigurator<T> configureQualifier(Class<T> qualifier) {
        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier class cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Configuring qualifier: " + qualifier.getSimpleName());

        // Create an AnnotatedType for the qualifier annotation class
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(qualifier);

        return new AnnotatedTypeConfiguratorImpl<T>(annotatedType) {
            @Override
            public AnnotatedType<T> complete() {
                AnnotatedType<T> configured = super.complete();

                // After configuration, register this as a qualifier if not already one
                if (!knowledgeBase.isRegisteredQualifier(qualifier)) {
                    knowledgeBase.addQualifier(qualifier);
                }

                messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Completed qualifier configuration: " +
                        qualifier.getSimpleName());
                return configured;
            }
        };
    }

    @Override
    public void addScope(Class<? extends Annotation> scopeType, boolean normal, boolean passivating) {
        if (scopeType == null) {
            throw new IllegalArgumentException("Scope type cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Adding scope: " + scopeType.getSimpleName() +
                " (normal=" + normal + ", passivating=" + passivating + ")");

        knowledgeBase.addScope(scopeType, normal, passivating);
    }

    @Override
    public void addStereotype(Class<? extends Annotation> stereotype, Annotation... stereotypeDef) {
        if (stereotype == null) {
            throw new IllegalArgumentException("Stereotype cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Adding stereotype: " + stereotype.getSimpleName() +
                " with meta-annotations: " + toList(stereotypeDef));

        // Register the stereotype with its meta-annotations in the knowledge base
        knowledgeBase.addStereotype(stereotype, stereotypeDef);
    }

    @Override
    public void addInterceptorBinding(AnnotatedType<? extends Annotation> bindingType) {
        if (bindingType == null) {
            throw new IllegalArgumentException("Interceptor binding AnnotatedType cannot be null");
        }

        // Extract meta-annotations from the AnnotatedType
        Annotation[] metaAnnotations = bindingType.getAnnotations().toArray(new Annotation[0]);

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Adding interceptor binding from AnnotatedType: " +
                bindingType.getJavaClass().getSimpleName() + " with meta-annotations: " + toList(metaAnnotations));

        // Register the interceptor binding with its meta-annotations
        knowledgeBase.addInterceptorBinding(bindingType.getJavaClass(), metaAnnotations);
    }

    @Override
    public void addInterceptorBinding(Class<? extends Annotation> bindingType, Annotation... bindingTypeDef) {
        if (bindingType == null) {
            throw new IllegalArgumentException("Interceptor binding type cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Adding interceptor binding: " + bindingType.getSimpleName() +
                " with meta-annotations: " + toList(bindingTypeDef));

        knowledgeBase.addInterceptorBinding(bindingType, bindingTypeDef);
    }

    /**
     * Return a configurator that will update the binding's metadata when complete() is called.
     * This allows extensions to add/modify annotations on the interceptor binding annotation type
     * (e.g., add @Nonbinding to specific members, add meta-annotations)
     * @param bindingType class of the interceptor binding annotation
     * @return an AnnotatedTypeConfigurator that can be used to configure the interceptor binding
     * @param <T> type of the interceptor binding annotation
     */
    @Override
    public <T extends Annotation> AnnotatedTypeConfigurator<T> configureInterceptorBinding(Class<T> bindingType) {
        if (bindingType == null) {
            throw new IllegalArgumentException("Interceptor binding type cannot be null");
        }

        messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Configuring interceptor binding: " +
                          bindingType.getSimpleName());

        // Create an AnnotatedType for the interceptor binding annotation class
        AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(bindingType);

        return new AnnotatedTypeConfiguratorImpl<T>(annotatedType) {
            @Override
            public AnnotatedType<T> complete() {
                AnnotatedType<T> configured = super.complete();

                // Extract any meta-annotations from the configured type
                Annotation[] metaAnnotations = configured.getAnnotations().toArray(new Annotation[0]);

                // After configuration, register this as an interceptor binding if not already one
                if (!knowledgeBase.isRegisteredInterceptorBinding(bindingType)) {
                    knowledgeBase.addInterceptorBinding(bindingType, metaAnnotations);

                    messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Completed interceptor binding configuration: " +
                            bindingType.getSimpleName() + " with meta-annotations: " + toList(metaAnnotations));
                } else {
                    messageHandler.handleInfoMessage("[BeforeBeanDiscovery] Interceptor with meta-annotations " +
                            toList(metaAnnotations) + " already configured");
                }
                return configured;
            }
        };
    }

    @Nonnull
    private String toList(Annotation[] stereotypeDef) {
        String metaAnnotationList;
        if (stereotypeDef != null && stereotypeDef.length > 0) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < stereotypeDef.length; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                Annotation annotation = stereotypeDef[i];
                builder.append("@").append(annotation.annotationType().getSimpleName());
            }
            builder.append("]");
            metaAnnotationList = builder.toString();
        } else {
            metaAnnotationList = "[]";
        }
        return metaAnnotationList;
    }

}
