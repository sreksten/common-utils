package com.threeamigos.common.util.implementations.injection.arquillian;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;
import org.jboss.arquillian.test.spi.TestEnricher;

/**
 * Arquillian extension to register Syringe components.
 */
public class SyringeArquillianExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, SyringeWildFlyArquillianContainer.class);
        builder.service(DeploymentExceptionTransformer.class, SyringeDeploymentExceptionTransformer.class);
        builder.service(TestEnricher.class, SyringeTestEnricher.class);
    }
}
