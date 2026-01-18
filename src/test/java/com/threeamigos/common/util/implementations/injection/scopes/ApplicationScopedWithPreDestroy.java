package com.threeamigos.common.util.implementations.injection.scopes;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationScopedWithPreDestroy {
    private boolean destroyed = false;

    @PreDestroy
    public void destroy() {
        this.destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
