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

    private static final Logger log = Logger.getLogger(SyringeService.class.getName());

    @Override
    public void start(StartContext context) throws StartException {
        log.info("Starting Syringe Service...");
        // This service represents the subsystem's presence in the server.
        // Actual containers are typically created per-deployment by DUPs.
    }

    @Override
    public void stop(StopContext context) {
        log.info("Stopping Syringe Service...");
    }

    @Override
    public SyringeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
