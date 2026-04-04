package com.threeamigos.common.util.implementations.injection.cditcktests.full.alternative.veto;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VetoedAlternativeTest {

    @Test
    void mockAlternativeIsVetoed() {
        Bootstrap bootstrap = newSyringe();
        Syringe syringe = bootstrap.syringe;

        try {
            setupOrThrow(bootstrap);
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> interfaceBeans = beanManager.getBeans(PaymentProcessor.class);
            Bean<?> bean = beanManager.resolve(interfaceBeans);
            assertNotNull(
                    bean,
                    "Expected PaymentProcessor bean. interface=" + describe(interfaceBeans) +
                            ", impl=" + describe(beanManager.getBeans(PaymentProcessorImpl.class)) +
                            ", mock=" + describe(beanManager.getBeans(MockPaymentProcessorImpl.class))
            );
            assertEquals("paymentProcessorImpl", bean.getName());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void alternativeStereotypeIsVetoed() {
        Bootstrap bootstrap = newSyringe();
        Syringe syringe = bootstrap.syringe;

        try {
            setupOrThrow(bootstrap);
            BeanManager beanManager = syringe.getBeanManager();
            Set<Bean<?>> consumerBeans = beanManager.getBeans(Consumer.class);
            Bean<?> bean = beanManager.resolve(consumerBeans);
            assertNotNull(bean);
            assertEquals(RequestScoped.class, bean.getScope(), "Resolved consumer from " + describe(consumerBeans));
        } finally {
            syringe.shutdown();
        }
    }

    private Bootstrap newSyringe() {
        InMemoryMessageHandler messageHandler = new InMemoryMessageHandler();
        Syringe syringe = new Syringe(
                messageHandler,
                PaymentProcessor.class,
                PaymentProcessorImpl.class,
                MockPaymentProcessorImpl.class,
                Consumer.class,
                AlternativeConsumerProducer.class,
                AlternativeConsumerStereotype.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(VetoingExtension.class.getName());
        addBeansXmlAlternatives(syringe);
        return new Bootstrap(syringe, messageHandler);
    }

    private void setupOrThrow(Bootstrap bootstrap) {
        try {
            bootstrap.syringe.setup();
        } catch (DefinitionException e) {
            StringBuilder details = new StringBuilder("Syringe setup failed.");
            if (!bootstrap.messageHandler.getAllErrorMessages().isEmpty()) {
                details.append(" Errors: ");
                for (String error : bootstrap.messageHandler.getAllErrorMessages()) {
                    details.append('[').append(error).append("] ");
                }
            }
            if (!bootstrap.messageHandler.getAllExceptionMessages().isEmpty()) {
                details.append(" Exceptions: ");
                for (String exception : bootstrap.messageHandler.getAllExceptionMessages()) {
                    details.append('[').append(exception).append("] ");
                }
            }
            throw new AssertionError(details.toString(), e);
        }
    }

    private void addBeansXmlAlternatives(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<alternatives>" +
                "<class>" + MockPaymentProcessorImpl.class.getName() + "</class>" +
                "<stereotype>" + AlternativeConsumerStereotype.class.getName() + "</stereotype>" +
                "</alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private static final class Bootstrap {
        private final Syringe syringe;
        private final InMemoryMessageHandler messageHandler;

        private Bootstrap(Syringe syringe, InMemoryMessageHandler messageHandler) {
            this.syringe = syringe;
            this.messageHandler = messageHandler;
        }
    }

    private String describe(Set<Bean<?>> beans) {
        if (beans == null || beans.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (Bean<?> bean : beans) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(bean.getBeanClass().getName())
                    .append("{name=")
                    .append(bean.getName())
                    .append(",scope=")
                    .append(bean.getScope().getName())
                    .append("}");
            first = false;
        }
        builder.append(']');
        return builder.toString();
    }

    public interface PaymentProcessor {
        boolean checkouPayment();
    }

    @Named
    @RequestScoped
    public static class PaymentProcessorImpl implements PaymentProcessor {
        @Override
        public boolean checkouPayment() {
            return false;
        }
    }

    @Named
    @Alternative
    @RequestScoped
    public static class MockPaymentProcessorImpl implements PaymentProcessor {
        @Override
        public boolean checkouPayment() {
            return false;
        }
    }

    @RequestScoped
    public static class Consumer {
    }

    @ApplicationScoped
    public static class AlternativeConsumerProducer {
        @Produces
        @AlternativeConsumerStereotype
        public Consumer createAlternativeConsumer() {
            return new Consumer();
        }
    }

    @Stereotype
    @Alternative
    @Dependent
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AlternativeConsumerStereotype {
    }

    public static class VetoingExtension implements Extension {
        public void observeMockPaymentProcessorPAT(@Observes ProcessAnnotatedType<MockPaymentProcessorImpl> event) {
            event.veto();
        }

        public void observeAlternativeConsumerPBA(@Observes ProcessBeanAttributes<AlternativeConsumerProducer> event) {
            event.veto();
        }
    }
}
