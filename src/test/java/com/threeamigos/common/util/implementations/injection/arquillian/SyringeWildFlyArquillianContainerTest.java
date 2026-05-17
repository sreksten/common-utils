package com.threeamigos.common.util.implementations.injection.arquillian;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyringeWildFlyArquillianContainerTest {

    @Test
    void shouldDetectNestedWildFlyDuplicateDeploymentFailure() {
        Exception root = new Exception("WFLYCTL0212: Duplicate resource [(\"deployment\" => \"foo.war\")]");
        Exception wrapped = new Exception("Could not deploy", root);

        assertTrue(SyringeWildFlyArquillianContainer.isDuplicateDeploymentFailure(wrapped));
    }

    @Test
    void shouldNotTreatUnrelatedDeploymentFailureAsDuplicate() {
        Exception root = new Exception("WFLYCTL0080: Failed services");
        Exception wrapped = new Exception("Could not deploy", root);

        assertFalse(SyringeWildFlyArquillianContainer.isDuplicateDeploymentFailure(wrapped));
    }
}
