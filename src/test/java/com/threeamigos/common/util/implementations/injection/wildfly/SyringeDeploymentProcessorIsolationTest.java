package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter19.par193interceptorresolution.InterceptorResolutionInCDIFullTest;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter20.par201decoratorbeans.DecoratorBeansTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyringeDeploymentProcessorIsolationTest {

    @Test
    @DisplayName("Chapter 27.2 deployment isolation: hashed archive keeps only anchor package classes")
    void shouldNarrowHashedArchiveToAnchorPackage() {
        Set<Class<?>> candidates = new HashSet<Class<?>>();
        candidates.add(InterceptorResolutionInCDIFullTest.class);
        candidates.add(DecoratorBeansTest.class);

        String deploymentName = "InterceptorResolutionInCDIFullTest1234567890abcdef1234567890abcdef12345678.war";
        Set<Class<?>> filtered = SyringeDeploymentProcessor.narrowToDeploymentScope(candidates, deploymentName);

        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(InterceptorResolutionInCDIFullTest.class));
    }

    @Test
    @DisplayName("Chapter 27.2 deployment isolation: non-hashed archives keep original candidate set")
    void shouldKeepOriginalCandidatesForNonHashedArchiveNames() {
        Set<Class<?>> candidates = new HashSet<Class<?>>();
        candidates.add(InterceptorResolutionInCDIFullTest.class);
        candidates.add(DecoratorBeansTest.class);

        Set<Class<?>> filtered = SyringeDeploymentProcessor.narrowToDeploymentScope(candidates, "myapp.war");
        assertEquals(candidates, filtered);
    }

    @Test
    @DisplayName("Chapter 27.2 deployment isolation: package prefix resolves from hashed archive anchor class")
    void shouldResolveScopedPackagePrefixFromIndexedClassNames() {
        List<String> indexed = Arrays.asList(
                "org.jboss.as.server.mgmt.domain.HostControllerConnection$ClientCallbackHandler",
                "org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.array.ArrayTest",
                "org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.array.ArrayProducer",
                "org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.finalClass.FinalClassTest"
        );

        String scoped = SyringeDeploymentProcessor.resolveScopedPackagePrefix(
                indexed,
                "ArrayTest36312725acb4ceb82ed417fd7b3ba68ddd07ab8.war");

        assertEquals("org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.array", scoped);
    }

    @Test
    @DisplayName("Chapter 27.2 deployment isolation: no scoped package for non-hashed deployment name")
    void shouldNotResolveScopedPackagePrefixForNonHashedDeploymentName() {
        List<String> indexed = Arrays.asList(
                "org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.array.ArrayTest",
                "org.jboss.cdi.tck.tests.lookup.clientProxy.unproxyable.array.ArrayProducer"
        );
        assertNull(SyringeDeploymentProcessor.resolveScopedPackagePrefix(indexed, "myapp.war"));
    }
}
