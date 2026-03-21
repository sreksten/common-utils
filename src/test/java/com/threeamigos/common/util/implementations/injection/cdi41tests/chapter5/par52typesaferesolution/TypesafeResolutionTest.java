package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par52typesaferesolution;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par52typesaferesolution.ambiguousresolutionbean.AmbiguousInterface;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter5.par52typesaferesolution.unresolvablebean.UnresolvableInterface;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.ConsoleMessageHandler;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName( "5.2 - Typesafe resolution tests")
public class TypesafeResolutionTest {

    @Test
    @DisplayName("5.2 - Should find an unresolvable dependency")
    void shouldFindAnUnresolvableDependency() {
        Syringe syringe = new Syringe(new ConsoleMessageHandler(), UnresolvableInterface.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        assertThrows(DeploymentException.class, syringe::setup);
        assertTrue(
                syringe.getKnowledgeBase().getInjectionErrors().stream()
                        .anyMatch(error -> error.contains("unsatisfied dependency - no bean found")),
                "Expected unsatisfied programmatic lookup to be registered as injection error"
        );
    }

    @Test
    @DisplayName("5.2 - Should find an ambiguous dependency")
    void shouldFindAnAmbiguousDependency() {
        Syringe syringe = new Syringe(new ConsoleMessageHandler(), AmbiguousInterface.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        assertThrows(DeploymentException.class, syringe::setup);
        assertTrue(
                syringe.getKnowledgeBase().getInjectionErrors().stream()
                        .anyMatch(error -> error.contains("ambiguous dependency - multiple beans found for type")),
                "Expected ambiguous programmatic lookup to be registered as injection error"
        );
    }
}
