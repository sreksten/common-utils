package com.threeamigos.common.util.implementations.injection.scopes;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ConversationScoped;

@ConversationScoped
public class ConversationScopedWithPreDestroy {
    private boolean preDestroyCalled = false;

    @PreDestroy
    public void destroy() {
        preDestroyCalled = true;
    }

    public boolean isPreDestroyCalled() {
        return preDestroyCalled;
    }
}
