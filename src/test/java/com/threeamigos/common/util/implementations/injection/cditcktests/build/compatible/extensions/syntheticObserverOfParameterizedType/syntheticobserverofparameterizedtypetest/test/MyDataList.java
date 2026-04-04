package com.threeamigos.common.util.implementations.injection.cditcktests.build.compatible.extensions.syntheticObserverOfParameterizedType.syntheticobserverofparameterizedtypetest.test;

import java.util.ArrayList;
import java.util.Collections;

public class MyDataList extends ArrayList<MyData> {

    public MyDataList(MyData first, MyData second) {
        super(2);
        Collections.addAll(this, first, second);
    }
}
