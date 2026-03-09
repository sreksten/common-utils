package com.threeamigos.common.util.implementations.injection.wildfly;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * DeploymentUnitProcessor that adds the Syringe module dependency to deployments.
 */
public class SyringeDependencyProcessor implements DeploymentUnitProcessor {

    private static final String SYRINGE_MODULE = "com.threeamigos.common.util";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        // Add the Syringe module as a dependency to the deployment
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.fromString(SYRINGE_MODULE), false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // No cleanup needed
    }
}
