package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import com.threeamigos.common.util.implementations.injection.spi.configurators.InjectionPointConfiguratorImpl;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.configurator.InjectionPointConfigurator;

/**
 * ProcessInjectionPoint event implementation.
 */
public class ProcessInjectionPointImpl<T, X> extends PhaseAware implements ProcessInjectionPoint<T, X> {

    private InjectionPoint injectionPoint;
    private final KnowledgeBase knowledgeBase;

    public ProcessInjectionPointImpl(MessageHandler messageHandler, InjectionPoint injectionPoint,
                                     KnowledgeBase knowledgeBase) {
        super(messageHandler);
        checkNotNull(injectionPoint, "InjectionPoint");
        this.injectionPoint = injectionPoint;
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

    @Override
    public void setInjectionPoint(InjectionPoint injectionPoint) {
        checkNotNull(injectionPoint, "InjectionPoint");
        info(Phase.PROCESS_INJECTION_POINT, "Changing injection point for " + injectionPoint.getMember());
        this.injectionPoint = injectionPoint;
    }

    @Override
    public InjectionPointConfigurator configureInjectionPoint() {
        info(Phase.PROCESS_INJECTION_POINT, "Configuring injection point for " + injectionPoint.getMember());
        return new InjectionPointConfiguratorImpl(injectionPoint) {
            @Override
            public InjectionPoint complete() {
                InjectionPoint configured = super.complete();
                setInjectionPoint(configured);
                return configured;
            }
        };
    }

    @Override
    public void addDefinitionError(Throwable t) {
        knowledgeBase.addDefinitionError(Phase.PROCESS_INJECTION_POINT, "Definition error for " +
                injectionPoint.getMember(), t);
    }
}
