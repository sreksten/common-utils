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
    private static final String TRANSACTION_API_MODULE = "jakarta.transaction.api";
    private static final String SERVLET_API_MODULE = "jakarta.servlet.api";
    private static final String JBOSS_AS_CONTROLLER_MODULE = "org.jboss.as.controller";
    private static final String JBOSS_AS_CONTROLLER_CLIENT_MODULE = "org.jboss.as.controller-client";
    private static final String JBOSS_AS_SERVER_MODULE = "org.jboss.as.server";
    private static final String JBOSS_AS_NAMING_MODULE = "org.jboss.as.naming";
    private static final String MSC_MODULE = "org.jboss.msc";
    private static final String JBOSS_MODULES_MODULE = "org.jboss.modules";
    private static final String DMR_MODULE = "org.jboss.dmr";
    private static final String STAXMAPPER_MODULE = "org.jboss.staxmapper";
    private static final String JAXB_API_MODULE = "jakarta.xml.bind.api";
    private static final String GUICE_MODULE = "com.google.inject";
    private static final String SNAKEYAML_MODULE = "org.yaml.snakeyaml";

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
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, TRANSACTION_API_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, SERVLET_API_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JBOSS_AS_CONTROLLER_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JBOSS_AS_CONTROLLER_CLIENT_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JBOSS_AS_SERVER_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JBOSS_AS_NAMING_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, MSC_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JBOSS_MODULES_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, DMR_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, STAXMAPPER_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, JAXB_API_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, GUICE_MODULE, false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, SNAKEYAML_MODULE, false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // No cleanup needed
    }
}
