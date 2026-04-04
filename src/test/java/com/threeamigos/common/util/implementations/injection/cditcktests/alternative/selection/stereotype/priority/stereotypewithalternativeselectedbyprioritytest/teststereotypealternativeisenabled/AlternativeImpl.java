package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.stereotype.priority.stereotypewithalternativeselectedbyprioritytest.teststereotypealternativeisenabled;

import jakarta.annotation.Priority;

@Priority(1000)
@AlternativeStereotype
public class AlternativeImpl implements SomeInterface {

    @Override
    public String ping() {
        return AlternativeImpl.class.getSimpleName();
    }
}
