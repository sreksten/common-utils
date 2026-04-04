package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.stereotype.priority.stereotypewithalternativeselectedbyprioritytest.teststereotypealternativeisenabled;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StandardImpl implements SomeInterface {

    @Override
    public String ping() {
        return StandardImpl.class.getSimpleName();
    }
}
