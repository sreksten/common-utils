package com.threeamigos.common.util.implementations.injection.cditcktests.implementation.disposal.method.definition;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;

@Dependent
public class DisposalNonBean {

    private static boolean tarantulaDestroyed = false;
    private static boolean webSpiderdestroyed = false;

    public DisposalNonBean(String someString) {
    }

    public void destroyDeadliestTarantula(@Disposes @Deadliest Tarantula spider) {
        tarantulaDestroyed = true;
    }

    public static boolean isTarantulaDestroyed() {
        return tarantulaDestroyed;
    }

    public static boolean isWebSpiderdestroyed() {
        return webSpiderdestroyed;
    }

    public static void setWebSpiderdestroyed(boolean webSpiderdestroyed) {
        DisposalNonBean.webSpiderdestroyed = webSpiderdestroyed;
    }
}
