package com.threeamigos.common.util.implementations.injection.arquillian;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Arquillian container adapter for Syringe in WildFly.
 */
public class SyringeWildFlyArquillianContainer implements DeployableContainer<SyringeWildFlyConfiguration> {

    private static final SyringeDeploymentExceptionTransformer DEPLOYMENT_EXCEPTION_TRANSFORMER =
            new SyringeDeploymentExceptionTransformer();

    private SyringeWildFlyConfiguration configuration;
    private ModelControllerClient client;

    @Override
    public Class<SyringeWildFlyConfiguration> getConfigurationClass() {
        return SyringeWildFlyConfiguration.class;
    }

    @Override
    public void setup(SyringeWildFlyConfiguration configuration) {
        configuration.validate();
        this.configuration = configuration;
        this.client = createClient(configuration);
    }

    @Override
    public void start() throws LifecycleException {
        if (configuration.isManaged()) {
            throw new LifecycleException("Managed startup not implemented; start WildFly externally or set managed=false");
        }
        // Remote mode: nothing to start; deployment will validate connectivity
    }

    @Override
    public void stop() throws LifecycleException {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                throw new LifecycleException("Failed to close management client", e);
            }
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        // Must match arquillian-protocol-servlet-jakarta expectation
        return new ProtocolDescription("Servlet 5.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        ensureClient();
        String runtimeName = archive.getName();

        byte[] content = toByteArray(archive);
        ServerDeploymentHelper helper = new ServerDeploymentHelper(client);
        try {
            helper.deploy(runtimeName, new ByteArrayInputStream(content));
        } catch (Exception e) {
            Throwable transformed = DEPLOYMENT_EXCEPTION_TRANSFORMER.transform(e);
            if (transformed instanceof RuntimeException
                    && (transformed instanceof jakarta.enterprise.inject.spi.DefinitionException
                    || transformed instanceof jakarta.enterprise.inject.spi.DeploymentException)) {
                throw (RuntimeException) transformed;
            }
            throw new DeploymentException("Could not deploy archive " + runtimeName, e);
        }

        ProtocolMetaData metaData = new ProtocolMetaData();
        // Arquillian servlet protocol resolves base URI by servlet name + context root.
        String contextRoot = deriveContextRoot(runtimeName);
        HTTPContext httpContext = new HTTPContext(configuration.getManagementHost(), configuration.getHttpPort());
        httpContext.add(new Servlet("ArquillianServletRunnerEE9", contextRoot));
        // Keep legacy name for compatibility with older protocol artifacts.
        httpContext.add(new Servlet("ArquillianServletRunner", contextRoot));
        metaData.addContext(httpContext);
        return metaData;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        ensureClient();
        String runtimeName = archive.getName();
        ServerDeploymentHelper helper = new ServerDeploymentHelper(client);
        try {
            helper.undeploy(runtimeName);
        } catch (Exception e) {
            throw new DeploymentException("Could not undeploy archive " + runtimeName, e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new DeploymentException("Descriptor deployment not supported");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new DeploymentException("Descriptor undeployment not supported");
    }

    private ModelControllerClient createClient(SyringeWildFlyConfiguration cfg) {
        try {
            if (cfg.getUsername() != null) {
                return ModelControllerClient.Factory.create(
                        InetAddress.getByName(cfg.getManagementHost()),
                        cfg.getManagementPort(),
                        new SimpleCallbackHandler(cfg.getUsername(), cfg.getPassword().toCharArray()));
            }
            return ModelControllerClient.Factory.create(
                    InetAddress.getByName(cfg.getManagementHost()),
                    cfg.getManagementPort());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create management client", e);
        }
    }

    private void ensureClient() throws DeploymentException {
        if (client == null) {
            throw new DeploymentException("Management client not initialized; did you call setup()?");
        }
    }

    private static byte[] toByteArray(Archive<?> archive) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        archive.as(ZipExporter.class).exportTo(baos);
        return baos.toByteArray();
    }

    private static String deriveContextRoot(String runtimeName) {
        if (runtimeName == null || runtimeName.trim().isEmpty()) {
            return "/";
        }
        int idx = runtimeName.lastIndexOf('.');
        String root = idx > 0 ? runtimeName.substring(0, idx) : runtimeName;
        if (!root.startsWith("/")) {
            root = "/" + root;
        }
        return root;
    }

    /**
     * Simple username/password callback handler for management authentication.
     */
    private static final class SimpleCallbackHandler implements CallbackHandler {
        private final String username;
        private final char[] password;

        private SimpleCallbackHandler(String username, char[] password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(username);
                } else if (callback instanceof PasswordCallback) {
                    ((PasswordCallback) callback).setPassword(password);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }
    }
}
