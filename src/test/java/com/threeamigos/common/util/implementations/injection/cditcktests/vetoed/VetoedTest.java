package com.threeamigos.common.util.implementations.injection.cditcktests.vetoed;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.vetoed.aquarium.Fishy;
import com.threeamigos.common.util.implementations.injection.cditcktests.vetoed.aquarium.Piranha;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VetoedTest {

    @Test
    void testClassLevelVeto() {
        Syringe syringe = new Syringe("com.threeamigos.common.util.implementations.injection.cditcktests.vetoed");
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            assertEquals(0, syringe.getBeanManager().getBeans(Elephant.class, Any.Literal.INSTANCE).size());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testPackageLevelVeto() {
        Syringe syringe = new Syringe("com.threeamigos.common.util.implementations.injection.cditcktests.vetoed");
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            assertEquals(0, syringe.getBeanManager().getBeans(Piranha.class, Any.Literal.INSTANCE).size());
            assertEquals(1, syringe.getBeanManager().getBeans(Shark.class, Any.Literal.INSTANCE).size());
            assertEquals(1, syringe.getBeanManager().getBeans(Shark.class, new Fishy.Literal()).size());
        } finally {
            syringe.shutdown();
        }
    }
}
