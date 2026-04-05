package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.processBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ActionSequence {

    private final List<String> data = new ArrayList<String>();

    void add(String action) {
        data.add(action);
    }

    List<String> getData() {
        return Collections.unmodifiableList(new ArrayList<String>(data));
    }
}
