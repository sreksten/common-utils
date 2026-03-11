package com.threeamigos.common.util.implementations.injection.wildfly;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.logging.Logger;

/**
 * MSC Service that manages a global Syringe instance (if needed) or
 * the shared infrastructure for deployment-specific containers.
 *
 * @author Stefano Reksten
 */
public class SyringeService implements Service<SyringeService> {

    @Override
    public void start(StartContext context) throws StartException {
        System.out.println("Starting Syringe Service...");
        // This service represents the subsystem's presence in the server.
        // Actual containers are typically created per-deployment by DUPs.
    }

    @Override
    public void stop(StopContext context) {
        System.out.println("Stopping Syringe Service...");
    }

    @Override
    public SyringeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
