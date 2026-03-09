package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Common attachment keys used by the Syringe subsystem.
 */
public class SyringeAttachments {

    /**
     * The Syringe container instance associated with a deployment.
     */
    public static final AttachmentKey<Syringe> SYRINGE_CONTAINER = AttachmentKey.create(Syringe.class);

    private SyringeAttachments() {
    }
}
