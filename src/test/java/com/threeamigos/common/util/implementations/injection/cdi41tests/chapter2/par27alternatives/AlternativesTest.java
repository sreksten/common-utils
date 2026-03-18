package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par27alternatives;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par27alternatives.bullet1.AlternativePaymentProcessor;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par27alternatives.bullet1.PaymentService;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par27alternatives.bullet2.CheckoutService;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par27alternatives.bullet3.AlternativeClient;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par27alternatives.bullet3.StereotypedAlternativeService;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par27alternatives.nonportableinterceptor.AlternativeInterceptor;
import com.threeamigos.common.util.implementations.injection.discovery.NonPortableBehaviourException;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Paragraph 2.8 - Alternatives")
public class AlternativesTest {

    @Test
    @DisplayName("2.8 - @Alternative bean is not selected unless enabled")
    void alternativeBeanIsNotSelectedUnlessEnabled() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), PaymentService.class);
        syringe.setup();

        PaymentService service = syringe.inject(PaymentService.class);
        assertEquals("standard", service.processorType());

        Bean<?> alternativeBean = findManagedBean(syringe, AlternativePaymentProcessor.class);
        assertTrue(alternativeBean.isAlternative());
    }

    @Test
    @DisplayName("2.8 - Programmatically enabled alternative is selected")
    void programmaticallyEnabledAlternativeIsSelected() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), PaymentService.class);
        syringe.enableAlternative(AlternativePaymentProcessor.class);
        syringe.setup();

        PaymentService service = syringe.inject(PaymentService.class);
        assertEquals("alternative", service.processorType());
    }

    @Test
    @DisplayName("2.8 - @Priority alternatives are enabled and highest priority wins")
    void priorityAlternativesAreEnabledAndHighestPriorityWins() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), CheckoutService.class);
        syringe.setup();

        CheckoutService checkoutService = syringe.inject(CheckoutService.class);
        assertEquals("highPriorityAlternative", checkoutService.gatewayType());
    }

    @Test
    @DisplayName("2.8 - Alternative declared by stereotype can be enabled programmatically")
    void alternativeDeclaredByStereotypeCanBeEnabledProgrammatically() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), AlternativeClient.class);
        syringe.enableAlternative(StereotypedAlternativeService.class);
        syringe.setup();

        AlternativeClient client = syringe.inject(AlternativeClient.class);
        assertEquals("stereotypedAlternative", client.serviceType());
    }

    @Test
    @DisplayName("2.8 - Alternative interceptor is non-portable")
    void alternativeInterceptorIsNonPortable() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), AlternativeInterceptor.class);

        assertThrows(NonPortableBehaviourException.class, syringe::setup);
    }

    private Bean<?> findManagedBean(Syringe syringe, Class<?> beanClass) {
        return syringe.getKnowledgeBase().getBeans().stream()
                .filter(bean -> bean.getBeanClass().equals(beanClass))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bean not found: " + beanClass.getName()));
    }
}
