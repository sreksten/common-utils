package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * DeploymentUnitProcessor that registers the Syringe BeanManager in JNDI.
 */
public class SyringeJndiBinderProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Syringe syringe = deploymentUnit.getAttachment(SyringeAttachments.SYRINGE_CONTAINER);

        if (syringe == null) {
            return;
        }

        BeanManager beanManager = syringe.getBeanManager();
        if (beanManager == null) {
            return;
        }

        final ServiceName beanManagerServiceName = ContextNames.JAVA_CONTEXT_SERVICE_NAME
                .append("comp")
                .append("BeanManager");

        final BinderService binderService = new BinderService("BeanManager", beanManager);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ServiceBuilder<?> builder = serviceTarget.addService(beanManagerServiceName, binderService);
        builder.install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Unbinding logic if necessary
    }
}
