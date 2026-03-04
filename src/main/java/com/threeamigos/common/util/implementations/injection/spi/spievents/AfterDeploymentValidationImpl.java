package com.threeamigos.common.util.implementations.injection.spi.spievents;

import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.spi.Phase;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;

/**
 * AfterDeploymentValidation event implementation.
 * 
 * <p>Fired after the container has validated all beans, injection points, and deployment descriptors.
 * Extensions can use this event to:
 * <ul>
 *   <li>Perform additional validation</li>
 *   <li>Register deployment problems via {@link #addDeploymentProblem(Throwable)}</li>
 * </ul>
 *
 * <p>This is the last event fired before the container is ready for use.
 *
 * @see jakarta.enterprise.inject.spi.AfterDeploymentValidation
 */
public class AfterDeploymentValidationImpl implements AfterDeploymentValidation {

    private final KnowledgeBase knowledgeBase;

    public AfterDeploymentValidationImpl(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public void addDeploymentProblem(Throwable t) {
        knowledgeBase.addError(Phase.AFTER_DEPLOYMENT_VALIDATION, "Deployment problem from extension: ", t);
    }
}
