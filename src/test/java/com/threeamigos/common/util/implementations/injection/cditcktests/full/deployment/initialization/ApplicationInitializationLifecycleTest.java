package com.threeamigos.common.util.implementations.injection.cditcktests.full.deployment.initialization;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationInitializationLifecycleTest {

    @Test
    void testInitialization() {
        ActionSequence.reset();

        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Foo.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(LifecycleMonitoringExtension.class.getName());
        syringe.setup();
        try {
            Foo foo = getContextualReference(syringe, Foo.class);
            foo.ping();

            List<String> correctSequenceData = new ArrayList<String>();
            correctSequenceData.add(LifecycleMonitoringExtension.class.getName());
            correctSequenceData.add(BeforeBeanDiscovery.class.getName());
            correctSequenceData.add(ProcessAnnotatedType.class.getName());
            correctSequenceData.add(AfterTypeDiscovery.class.getName());
            correctSequenceData.add(AfterBeanDiscovery.class.getName());
            correctSequenceData.add(AfterDeploymentValidation.class.getName());
            correctSequenceData.add(Foo.class.getName());

            assertEquals(correctSequenceData, ActionSequence.getSequenceData());
        } finally {
            syringe.shutdown();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T getContextualReference(Syringe syringe, Class<T> type) {
        jakarta.enterprise.inject.spi.BeanManager beanManager = syringe.getBeanManager();
        jakarta.enterprise.inject.spi.Bean<T> bean = (jakarta.enterprise.inject.spi.Bean<T>) beanManager.resolve((java.util.Set) beanManager.getBeans(type));
        return type.cast(beanManager.getReference(bean, type, beanManager.createCreationalContext(bean)));
    }
}
