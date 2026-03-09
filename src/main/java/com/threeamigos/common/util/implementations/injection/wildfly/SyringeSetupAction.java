package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import org.jboss.as.server.deployment.SetupAction;

/**
 * SyringeSetupAction - Manages the ThreadLocal CDI context for Syringe in WildFly.
 *
 * <p>This implementation ensures that {@code CDI.current()} returns the correct
 * Syringe instance for the current deployment during the execution of a request.
 *
 * @author Stefano Reksten
 */
public class SyringeSetupAction implements SetupAction {

    private final Syringe syringe;

    public SyringeSetupAction(Syringe syringe) {
        this.syringe = syringe;
    }

    @Override
    public void setup(java.util.Map<String, Object> properties) {
        SyringeCDIProvider.registerThreadLocalCDI(syringe.getCDI());
    }

    @Override
    public void teardown(java.util.Map<String, Object> properties) {
        SyringeCDIProvider.unregisterThreadLocalCDI();
    }

    @Override
    public java.util.Set<org.jboss.msc.service.ServiceName> dependencies() {
        return java.util.Collections.emptySet();
    }

    @Override
    public int priority() {
        return 100; // Standard priority
    }
}
