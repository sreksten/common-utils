package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;

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

        // Use per-deployment bind info to avoid clashes and ensure the parent naming store is wired
        final BindInfo bindInfo = ContextNames.bindInfoFor("java:comp/BeanManager");

        final BinderService binderService = new BinderService(bindInfo.getBindName());
        binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(beanManager)));

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ServiceBuilder<?> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());

        builder.install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // The BinderService is tied to the deployment's naming store; it will
        // be removed automatically with the deployment service lifecycle. No explicit
        // teardown required here.
    }
}
