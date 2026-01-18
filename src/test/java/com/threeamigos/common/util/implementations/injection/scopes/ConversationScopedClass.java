package com.threeamigos.common.util.implementations.injection.scopes;

import javax.enterprise.context.ConversationScoped;

@ConversationScoped
public class ConversationScopedClass {
    private final long instanceId = System.nanoTime();

    public long getInstanceId() {
        return instanceId;
    }
}
