package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType.syntheticobserverofparameterizedtypetest.test;

import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.spi.EventContext;

import java.util.ArrayList;
import java.util.List;

public class MyObserver implements SyntheticObserver<List<MyData>> {

    private static final List<String> OBSERVED = new ArrayList<String>();

    @Override
    public void observe(EventContext<List<MyData>> event, Parameters params) {
        StringBuilder text = new StringBuilder();
        List<MyData> data = event.getEvent();
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                text.append(' ');
            }
            text.append(data.get(i).getPayload());
        }
        OBSERVED.add(text.toString());
    }

    public static void reset() {
        OBSERVED.clear();
    }

    public static List<String> getObserved() {
        return new ArrayList<String>(OBSERVED);
    }
}
