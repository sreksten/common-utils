package com.threeamigos.common.util.implementations.injection.cditcktests.definition.stereotype.broken.multiplePriorities.conflictingprioritystereotypestest.test;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

@Dependent
@Alternative
@PriorityStereotype
@PriorityStereotype2
public class SomeBean {
}
