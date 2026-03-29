package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter19.par193interceptorresolution.InterceptorResolutionInCDIFullTest;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter20.par201decoratorbeans.DecoratorBeansTest;
import com.threeamigos.common.util.implementations.injection.wildfly.subpackage.DynamicLookupBrokenSubpackageBean;
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
    @DisplayName("Chapter 27.2 deployment isolation: hashed archive excludes subpackages of anchor package")
    void shouldExcludeSubpackagesFromHashedArchiveScope() {
        Set<Class<?>> candidates = new HashSet<Class<?>>();
        candidates.add(DynamicLookupTestAnchor.class);
        candidates.add(DynamicLookupBrokenSubpackageBean.class);

        String deploymentName = "DynamicLookupTestAnchor1234567890abcdef1234567890abcdef12345678.war";
        Set<Class<?>> filtered = SyringeDeploymentProcessor.narrowToDeploymentScope(candidates, deploymentName);

        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(DynamicLookupTestAnchor.class));
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

    @Test
    @DisplayName("Chapter 27.2 deployment isolation: local deployment index has priority over composite index")
    void shouldPreferLocalIndexWhenSelectingIndexedClassNames() {
        List<String> local = Arrays.asList(
                "org.jboss.cdi.tck.tests.lookup.dynamic.DynamicLookupTest",
                "org.jboss.cdi.tck.tests.lookup.dynamic.ObtainsInstanceBean"
        );
        List<String> composite = Arrays.asList(
                "org.jboss.cdi.tck.tests.lookup.dynamic.DynamicLookupTest",
                "org.jboss.cdi.tck.tests.lookup.dynamic.broken.raw.FieldInjectionBar"
        );

        List<String> selected = SyringeDeploymentProcessor.selectIndexedClassNames(local, composite);
        assertEquals(local, selected);
    }

    @Test
    @DisplayName("Chapter 27.2 deployment isolation: composite index is used only when local index is empty")
    void shouldFallbackToCompositeIndexWhenLocalIsEmpty() {
        List<String> selected = SyringeDeploymentProcessor.selectIndexedClassNames(
                java.util.Collections.<String>emptyList(),
                Arrays.asList("org.jboss.cdi.tck.tests.lookup.dynamic.DynamicLookupTest"));

        assertEquals(1, selected.size());
        assertEquals("org.jboss.cdi.tck.tests.lookup.dynamic.DynamicLookupTest", selected.get(0));
    }

    static class DynamicLookupTestAnchor {
    }

}
