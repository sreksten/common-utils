package com.threeamigos.common.util.implementations.injection.wildfly;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleLoader;

/**
 * DeploymentUnitProcessor that adds the Syringe module dependency to deployments.
 */
public class SyringeDependencyProcessor implements DeploymentUnitProcessor {

    private static final String SYRINGE_MODULE = "com.threeamigos.common.util";
    private static final String CDI_API_MODULE = "jakarta.enterprise.api";
    private static final String INJECT_API_MODULE = "jakarta.inject.api";
    private static final String ANNOTATION_API_MODULE = "jakarta.annotation.api";
    private static final String INTERCEPTOR_API_MODULE = "jakarta.interceptor.api";
    private static final String GUICE_MODULE = "com.google.inject";
    private static final String SNAKEYAML_MODULE = "org.yaml.snakeyaml";
    private static final String WELD_CORE_MODULE = "org.jboss.weld.core";
    private static final String MSC_MODULE = "org.jboss.msc";
    private static final String SHRINKWRAP_DESCRIPTORS_MODULE = "org.jboss.shrinkwrap.descriptors";
    private static final String ANT_MODULE = "org.apache.ant";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // Skip subdeployments (e.g., EAR modules) which inherit their parent's module spec
        if (deploymentUnit.getParent() != null) {
            return;
        }

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        // Attachments can be absent for non-EE or special deployments; guard to avoid NPEs
        if (moduleSpecification == null || moduleLoader == null) {
            return;
        }

        // Add the Syringe module and required CDI APIs as dependencies to the deployment.
        // Constructor signature in WildFly Core 31+ takes the module name as String.
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, SYRINGE_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, CDI_API_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, INJECT_API_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, ANNOTATION_API_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, INTERCEPTOR_API_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, GUICE_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, SNAKEYAML_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, WELD_CORE_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, MSC_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, SHRINKWRAP_DESCRIPTORS_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, ANT_MODULE, false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // No cleanup needed
    }
}
