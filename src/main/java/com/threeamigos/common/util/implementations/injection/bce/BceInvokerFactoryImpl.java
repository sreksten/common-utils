package com.threeamigos.common.util.implementations.injection.bce;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.resolution.BeanImpl;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import com.threeamigos.common.util.implementations.injection.spi.spievents.ProcessManagedBeanImpl;
import com.threeamigos.common.util.implementations.injection.spi.wrappers.AnnotatedMethodWrapper;
import com.threeamigos.common.util.interfaces.messagehandler.MessageHandler;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.InvokerFactory;
import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.invoke.Invoker;
import jakarta.enterprise.invoke.InvokerBuilder;
import jakarta.enterprise.lang.model.declarations.MethodInfo;

import java.lang.reflect.Method;

final class BceInvokerFactoryImpl implements InvokerFactory {

    private final KnowledgeBase knowledgeBase;
    private final BeanManagerImpl beanManager;
    private final MessageHandler messageHandler;
    private final BceInvokerRegistry invokerRegistry;

    BceInvokerFactoryImpl(KnowledgeBase knowledgeBase,
                          BeanManagerImpl beanManager,
                          MessageHandler messageHandler,
                          BceInvokerRegistry invokerRegistry) {
        this.knowledgeBase = knowledgeBase;
        this.beanManager = beanManager;
        this.messageHandler = messageHandler;
        this.invokerRegistry = invokerRegistry;
    }

    @Override
    public InvokerBuilder<InvokerInfo> createInvoker(BeanInfo bean, MethodInfo method) {
        Class<?> beanClass = BceMetadata.unwrapBeanClass(bean);
        Method javaMethod = BceMetadata.unwrapMethod(method);
        Bean<?> managedBean = findManagedBean(beanClass);
        if (!(managedBean instanceof BeanImpl<?>)) {
            throw new DefinitionException("BCE InvokerFactory target bean is not a managed bean: " + beanClass.getName());
        }

        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(beanClass);
        ProcessManagedBeanImpl<?> event = new ProcessManagedBeanImpl<>(
                messageHandler, knowledgeBase, (Bean<Object>) managedBean, (AnnotatedType<Object>) annotatedType, beanManager);
        AnnotatedMethodWrapper wrapper = new AnnotatedMethodWrapper(javaMethod, annotatedType);
        final InvokerBuilder<Invoker<Object, ?>> runtimeBuilder =
            (InvokerBuilder<Invoker<Object, ?>>) event.createInvoker(wrapper);

        return new InvokerBuilder<InvokerInfo>() {
            @Override
            public InvokerBuilder<InvokerInfo> withInstanceLookup() {
                runtimeBuilder.withInstanceLookup();
                return this;
            }

            @Override
            public InvokerBuilder<InvokerInfo> withArgumentLookup(int position) {
                runtimeBuilder.withArgumentLookup(position);
                return this;
            }

            @Override
            public InvokerInfo build() {
                Invoker<Object, ?> runtimeInvoker = runtimeBuilder.build();
                return invokerRegistry.register(runtimeInvoker);
            }
        };
    }

    private Bean<?> findManagedBean(Class<?> beanClass) {
        for (Bean<?> candidate : knowledgeBase.getBeans()) {
            if (candidate.getBeanClass().equals(beanClass)) {
                return candidate;
            }
        }
        throw new DefinitionException("No bean found for BCE InvokerFactory target class: " + beanClass.getName());
    }
}
