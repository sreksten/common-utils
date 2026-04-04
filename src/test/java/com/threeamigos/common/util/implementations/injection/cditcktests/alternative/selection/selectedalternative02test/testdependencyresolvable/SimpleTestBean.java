package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative02test.testdependencyresolvable;

public class SimpleTestBean implements TestBean {

    @Override
    public String getId() {
        return SimpleTestBean.class.getName();
    }
}
