package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jandex.ClassInfo;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.modules.Module;

import java.util.HashSet;
import java.util.Set;

/**
 * DeploymentUnitProcessor that initializes Syringe for each deployment.
 */
public class SyringeDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (index == null || module == null) {
            return;
        }

        // 1. Discover classes via Jandex
        Set<Class<?>> discoveredClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getKnownClasses()) {
            try {
                Class<?> clazz = module.getClassLoader().loadClass(classInfo.name().toString());
                discoveredClasses.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // Ignore classes that cannot be resolved (optional dependencies like Ant/TestNG tasks)
            }
        }

        if (discoveredClasses.isEmpty()) {
            return;
        }

        // 2. Initialize Syringe via Bootstrap
        SyringeBootstrap bootstrap = new SyringeBootstrap(discoveredClasses, module.getClassLoader());
        Syringe syringe = bootstrap.bootstrap();

        // 3. Attach Syringe to the deployment unit for later use (e.g., in Setup Actions)
        deploymentUnit.putAttachment(SyringeAttachments.SYRINGE_CONTAINER, syringe);

        // 4. Register SetupAction for CDI.current() support
        deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS, new SyringeSetupAction(syringe));
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Retrieve the Syringe instance and shut it down
        Syringe syringe = context.getAttachment(SyringeAttachments.SYRINGE_CONTAINER);
        if (syringe != null) {
            syringe.shutdown();
        }
    }
}
