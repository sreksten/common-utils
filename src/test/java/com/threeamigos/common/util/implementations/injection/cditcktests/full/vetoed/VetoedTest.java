package com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed.aquarium.Fish;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed.aquarium.FishType;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed.aquarium.Fishy;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.vetoed.aquarium.Piranha;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.inject.Any;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VetoedTest {

    @Test
    void testClassLevelVeto() {
        Fixture fixture = startFixture();
        try {
            assertFalse(fixture.verifyingExtension.getClasses().contains(Elephant.class));
            assertEquals(0, fixture.syringe.getBeanManager().getBeans(Elephant.class, Any.Literal.INSTANCE).size());
            assertFalse(fixture.verifyingExtension.getClasses().contains(Animal.class));
        } finally {
            fixture.syringe.shutdown();
        }
    }

    @Test
    void testPackageLevelVeto() {
        Fixture fixture = startFixture();
        try {
            assertFalse(fixture.verifyingExtension.getClasses().contains(Piranha.class));
            assertFalse(fixture.verifyingExtension.getClasses().contains(Fish.class));
            assertFalse(fixture.verifyingExtension.getClasses().contains(FishType.class));
            assertFalse(fixture.verifyingExtension.getClasses().contains(Fishy.class));
            assertTrue(fixture.verifyingExtension.getClasses().contains(Shark.class));
            assertEquals(0, fixture.syringe.getBeanManager().getBeans(Piranha.class, Any.Literal.INSTANCE).size());
            assertEquals(1, fixture.syringe.getBeanManager().getBeans(Shark.class, Any.Literal.INSTANCE).size());
            assertEquals(1, fixture.syringe.getBeanManager().getBeans(Shark.class, new Fishy.Literal()).size());
        } finally {
            fixture.syringe.shutdown();
        }
    }

    @Test
    void testAnnotatedTypeAddedByExtension() {
        Fixture fixture = startFixture();
        try {
            assertFalse(fixture.verifyingExtension.getClasses().contains(Gecko.class));
            assertEquals(0, fixture.syringe.getBeanManager().getBeans(Gecko.class, Any.Literal.INSTANCE).size());
            assertFalse(fixture.verifyingExtension.getClasses().contains(Reptile.class));
        } finally {
            fixture.syringe.shutdown();
        }
    }

    private Fixture startFixture() {
        VerifyingExtension verifyingExtension = new VerifyingExtension();

        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ModifyingExtension.class.getName());
        syringe.addExtension(verifyingExtension);
        syringe.initialize();

        syringe.addDiscoveredClass(Animal.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Elephant.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Shark.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Predator.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnimalStereotype.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Tiger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(ModifyingExtension.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(VerifyingExtension.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Gecko.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Reptile.class, BeanArchiveMode.EXPLICIT);

        syringe.addDiscoveredClass(Piranha.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Fish.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FishType.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Fishy.class, BeanArchiveMode.EXPLICIT);

        syringe.start();
        return new Fixture(syringe, verifyingExtension);
    }

    private static class Fixture {
        private final Syringe syringe;
        private final VerifyingExtension verifyingExtension;

        private Fixture(Syringe syringe, VerifyingExtension verifyingExtension) {
            this.syringe = syringe;
            this.verifyingExtension = verifyingExtension;
        }
    }
}
