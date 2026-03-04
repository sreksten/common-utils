package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
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
 * @param <T> the bean type
 * @see jakarta.enterprise.inject.spi.ProcessSyntheticBean
 */
public class ProcessSyntheticBeanImpl<T> extends PhaseAware implements ProcessSyntheticBean<T> {

    private final Bean<T> bean;
    private final Extension source;
    private final KnowledgeBase knowledgeBase;

    /**
     * Constructor for ProcessSyntheticBean event.
     *
     * @param bean the synthetic bean
     * @param source the extension that registered this bean (can be null if unknown)
     */
    public ProcessSyntheticBeanImpl(MessageHandler messageHandler, KnowledgeBase knowledgeBase, Bean<T> bean,
                                    Extension source) {
        super(messageHandler);
        this.knowledgeBase = knowledgeBase;
        this.bean = bean;
        this.source = source;
    }

    @Override
    public Bean<T> getBean() {
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
        knowledgeBase.addDefinitionError(Phase.PROCESS_SYNTHETIC_BEAN, "Definition error for " +
                         bean.getBeanClass().getName(), t);
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
