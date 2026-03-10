package com.threeamigos.common.util.implementations.injection.arquillian;

import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * Configuration for the Syringe WildFly container.
 *
 * <p>Supports remote mode (WildFly started externally). Managed startup can be added later.
 */
public class SyringeWildFlyConfiguration implements ContainerConfiguration {

    private String managementHost = "localhost";
    private int managementPort = 9990;
    private int httpPort = 8080;
    private String username;
    private String password;
    private boolean managed = false; // managed startup not implemented yet
    private String wildFlyHome;
    private String serverConfig = "standalone.xml";
    private int timeoutSeconds = 120;

    @Override
    public void validate() {
            if (isBlank(managementHost)) {
                throw new IllegalArgumentException("managementHost must be set");
            }
            if (managementPort <= 0) {
                throw new IllegalArgumentException("managementPort must be > 0");
            }
            if (httpPort <= 0) {
                throw new IllegalArgumentException("httpPort must be > 0");
            }
            if ((username == null) != (password == null)) {
                throw new IllegalArgumentException("Both username and password must be provided together");
            }
            if (managed) {
                if (isBlank(wildFlyHome)) {
                    throw new IllegalArgumentException("wildFlyHome must be set when managed=true");
                }
            }
    }

    public String getManagementHost() {
        return managementHost;
    }

    public void setManagementHost(String managementHost) {
        this.managementHost = managementHost;
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public String getWildFlyHome() {
        return wildFlyHome;
    }

    public void setWildFlyHome(String wildFlyHome) {
        this.wildFlyHome = wildFlyHome;
    }

    public String getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(String serverConfig) {
        this.serverConfig = serverConfig;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
