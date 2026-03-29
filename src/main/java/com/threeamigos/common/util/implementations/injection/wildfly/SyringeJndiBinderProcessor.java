package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;

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

        //FIXME
        // Bind BeanManager into the standard module namespace expected by CDI consumers.
        final String moduleName = simpleName(deploymentUnit.getName());
        final String appName = deploymentUnit.getParent() != null
                ? simpleName(deploymentUnit.getParent().getName())
                : moduleName;

        final BindInfo bindInfo;
        try {
            bindInfo = ContextNames.bindInfoFor(appName, moduleName, null, "java:module/BeanManager");
        } catch (RuntimeException e) {
            // If the namespace is rejected (e.g., illegal context), do not fail deployment;
            // just skip binding. CDI.current() and injection still work.
            NamingLogger.ROOT_LOGGER.debug("Skipping BeanManager JNDI binding", e);
            return;
        }

        // If another component already registered BeanManager binding,
        // do not attempt to register the same MSC service again.
        if (phaseContext.getServiceRegistry().getService(bindInfo.getBinderServiceName()) != null) {
            NamingLogger.ROOT_LOGGER.debugf("BeanManager binder service already present for deployment %s, skipping Syringe binding",
                    deploymentUnit.getName());
            return;
        }

        final BinderService binderService = new BinderService(bindInfo.getBindName());
        binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new Value<Object>() {
            @Override
            public Object getValue() {
                return beanManager;
            }
        }));

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final ServiceBuilder<?> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());

        builder.install();
    }

    private static String simpleName(String deploymentName) {
        if (deploymentName == null) {
            return "";
        }
        int dot = deploymentName.lastIndexOf('.');
        if (dot > 0) {
            return deploymentName.substring(0, dot);
        }
        return deploymentName;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // The BinderService is tied to the deployment's naming store; it will
        // be removed automatically with the deployment service lifecycle. No explicit
        // teardown required here.
    }
}
