package com.threeamigos.common.util.implementations.injection.cditcktests.full.lookup.byname;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class ResolutionByNameTest {

    @Test
    void testAmbiguousELNamesResolved() {
        Syringe syringe = new Syringe(
                new InMemoryMessageHandler(),
                AlaskaPlaice.class,
                Animal.class,
                Cod.class,
                Plaice.class,
                Salmon.class,
                Sole.class,
                Whitefish.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);

        try {
            syringe.setup();

            Set<Bean<?>> beans = syringe.getBeanManager().getBeans("whitefish");
            assertEquals(1, beans.size());
            assertTrue(syringe.getBeanManager().resolve(beans).getTypes().contains(AlaskaPlaice.class));

            beans = syringe.getBeanManager().getBeans("fish");
            assertEquals(2, beans.size());
            assertTrue(syringe.getBeanManager().resolve(beans).getTypes().contains(Sole.class));
        } finally {
            syringe.shutdown();
        }
    }
}
