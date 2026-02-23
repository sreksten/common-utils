package com.threeamigos.common.util.implementations.injection.spievents;

import jakarta.enterprise.inject.spi.*;

/**
 * ProcessSyntheticBean event implementation.
 *
 * <p>Fired for each synthetic bean registered via AfterBeanDiscovery.addBean().
 * Synthetic beans are not discovered via classpath scanning but are programmatically
 * registered by portable extensions.
 *
 * <p>Extensions can observe this event to:
 * <ul>
 *   <li>Inspect synthetic bean metadata</li>
 *   <li>Validate synthetic bean configuration</li>
 *   <li>Observe beans added by other extensions</li>
 * </ul>
 *
 * <p><b>Note:</b> Unlike other ProcessBean events (ProcessManagedBean, ProcessProducerMethod),
 * ProcessSyntheticBean does not provide setters or configurators since the bean
 * was already fully configured when it was added.
 *
 * @param <X> the bean type
 * @see jakarta.enterprise.inject.spi.ProcessSyntheticBean
 */
public class ProcessSyntheticBeanImpl<X> implements ProcessSyntheticBean<X> {

    private final Bean<X> bean;
    private final Extension source;
    private final BeanManager beanManager;
    private final com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase knowledgeBase;

    /**
     * Constructor for ProcessSyntheticBean event.
     *
     * @param bean the synthetic bean
     * @param source the extension that registered this bean (may be null if unknown)
     * @param beanManager the bean manager
     */
    public ProcessSyntheticBeanImpl(Bean<X> bean, Extension source, BeanManager beanManager) {
        this.bean = bean;
        this.source = source;
        this.beanManager = beanManager;
        // Extract KnowledgeBase from BeanManager
        if (beanManager instanceof com.threeamigos.common.util.implementations.injection.BeanManagerImpl) {
            this.knowledgeBase = ((com.threeamigos.common.util.implementations.injection.BeanManagerImpl) beanManager).getKnowledgeBase();
        } else {
            this.knowledgeBase = null;
        }
    }

    @Override
    public Bean<X> getBean() {
        return bean;
    }

    @Override
    public Annotated getAnnotated() {
        // Synthetic beans don't have an Annotated representation since they
        // weren't discovered via reflection
        return null;
    }

    @Override
    public Extension getSource() {
        return source;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        String errorMsg = "ProcessSyntheticBean definition error for " +
                         bean.getBeanClass().getName() + ": " +
                         (t != null ? t.getMessage() : "null");
        System.err.println("[ProcessSyntheticBean] " + errorMsg);

        if (knowledgeBase != null) {
            knowledgeBase.addDefinitionError(errorMsg);
        } else {
            System.err.println("[ProcessSyntheticBean] WARNING: Cannot propagate error - KnowledgeBase not available");
        }

        // Also print stack trace for debugging
        if (t != null) {
            t.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "ProcessSyntheticBean{" +
               "bean=" + bean.getBeanClass().getName() +
               ", types=" + bean.getTypes() +
               ", qualifiers=" + bean.getQualifiers() +
               '}';
    }
}
