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
            String className = classInfo.name().toString();
            if (shouldSkipInfrastructureClass(className)) {
                continue;
            }
            try {
                Class<?> clazz = module.getClassLoader().loadClass(className);
                discoveredClasses.add(clazz);
            } catch (ClassNotFoundException e) {
                // Ignore classes that cannot be resolved.
            } catch (LinkageError e) {
                // Ignore classes that fail to link because optional/transitive types are not visible.
            } catch (RuntimeException e) {
                // Keep discovery resilient for non-application classes in third-party libraries.
            }
        }

        if (discoveredClasses.isEmpty()) {
            return;
        }

        // 2. Initialize Syringe via Bootstrap
        SyringeBootstrap bootstrap = new SyringeBootstrap(discoveredClasses, module.getClassLoader());
        Syringe syringe = null;
        try {
            syringe = bootstrap.bootstrap();

            // 3. Attach Syringe to the deployment unit for later use (e.g., in Setup Actions)
            deploymentUnit.putAttachment(SyringeAttachments.SYRINGE_CONTAINER, syringe);

            // 4. Register SetupAction for CDI.current() support
            deploymentUnit.addToAttachmentList(org.jboss.as.server.deployment.Attachments.SETUP_ACTIONS, new SyringeSetupAction(syringe));
        } catch (RuntimeException e) {
            // Defensive cleanup on deployment failure before attachment is established.
            try {
                bootstrap.shutdown();
            } catch (Exception ignored) {
                // Best-effort cleanup.
            }
            throw e;
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // Retrieve the Syringe instance and shut it down
        Syringe syringe = context.getAttachment(SyringeAttachments.SYRINGE_CONTAINER);
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    private static boolean shouldSkipInfrastructureClass(String className) {
        return className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jakarta.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("org.jboss.arquillian.")
                || className.startsWith("org.jboss.shrinkwrap.")
                || className.startsWith("org.testng.")
                || className.startsWith("org.junit.")
                || className.startsWith("org.hamcrest.")
                || className.startsWith("org.apache.")
                || className.startsWith("org.slf4j.")
                || className.startsWith("ch.qos.logback.")
                || className.startsWith("org.jboss.logging.")
                || className.startsWith("com.threeamigos.common.util.implementations.injection.arquillian.");
    }
}
