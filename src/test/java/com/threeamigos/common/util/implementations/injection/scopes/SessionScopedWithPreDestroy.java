package com.threeamigos.common.util.implementations.injection.scopes;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

@SessionScoped
public class SessionScopedWithPreDestroy implements Serializable {
    private boolean destroyed = false;

    @PreDestroy
    public void destroy() {
        this.destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
