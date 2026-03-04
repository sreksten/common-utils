package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.spi.*;

/**
 * ProcessBean event implementation.
 * 
 * <p>Fired for each registered bean (managed beans, producer methods, producer fields).
 * Extensions can observe this event to:
 * <ul>
 *   <li>Inspect bean metadata</li>
 *   <li>Validate bean configuration</li>
 * </ul>
 *
 * @param <T> the bean class type
 * @see jakarta.enterprise.inject.spi.ProcessBean
 */
public class ProcessBeanImpl<T> extends PhaseAware implements ProcessBean<T> {

    protected final Bean<T> bean;
    private final Annotated annotated;
    protected final KnowledgeBase knowledgeBase;

    public ProcessBeanImpl(MessageHandler messageHandler, KnowledgeBase knowledgeBase, Bean<T> bean, Annotated annotated) {
        super(messageHandler);
        this.knowledgeBase = knowledgeBase;
        this.bean = bean;
        this.annotated = annotated;
    }

    @Override
    public Annotated getAnnotated() {
        return annotated;
    }

    @Override
    public Bean<T> getBean() {
        return bean;
    }

    @Override
    public void addDefinitionError(Throwable t) {
        knowledgeBase.addDefinitionError(Phase.PROCESS_BEAN, "Definition error for " +
                bean.getBeanClass().getName(), t);
    }
}
