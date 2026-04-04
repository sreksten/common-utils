package com.threeamigos.common.util.implementations.injection.cditcktests.alternative.selection.selectedalternative01test.testalternativeproducerselected;

import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.selection.Tame;
import com.threeamigos.common.util.implementations.injection.cditcktests.support.alternative.selection.Wild;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@Priority(1100)
@Dependent
public class BarProducer {

    @Alternative
    @Produces
    @Wild
    public final Bar producedBar = new Bar();

    @Alternative
    @Produces
    @Tame
    public Bar produceTameBar() {
        return new Bar();
    }
}
