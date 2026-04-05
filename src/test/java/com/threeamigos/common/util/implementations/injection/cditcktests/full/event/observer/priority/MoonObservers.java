package com.threeamigos.common.util.implementations.injection.cditcktests.full.event.observer.priority;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;

public class MoonObservers {

    @Dependent
    public static class Observer2 {
        public void observeMoon(@Observes Moonrise moonrise) {
            ActionSequence.addAction(getClass().getName());
        }
    }

    @Dependent
    public static class Observer3 {
        public void observeMoon(@Observes @Priority(Interceptor.Priority.APPLICATION + 900) MoonActivity moonActivity) {
            ActionSequence.addAction(getClass().getName());
        }
    }
}
