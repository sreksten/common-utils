package com.threeamigos.common.util.implementations.injection.spievents;

import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.BeanManager;

/**
 * BeforeShutdown event implementation.
 * 
 * <p>Fired when the container is about to shut down. Extensions can use this event to:
 * <ul>
 *   <li>Perform cleanup operations</li>
 *   <li>Release resources</li>
 *   <li>Log shutdown information</li>
 * </ul>
 *
 * <p>This is the last event fired during the container lifecycle.
 * After this event, all contexts are destroyed and @PreDestroy callbacks are invoked.
 *
 * @see jakarta.enterprise.inject.spi.BeforeShutdown
 */
public class BeforeShutdownImpl implements BeforeShutdown {

    private final BeanManager beanManager;

    public BeforeShutdownImpl(BeanManager beanManager) {
        this.beanManager = beanManager;
    }
}
