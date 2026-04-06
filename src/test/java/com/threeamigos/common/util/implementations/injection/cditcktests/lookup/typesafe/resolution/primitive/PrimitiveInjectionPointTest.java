package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.typesafe.resolution.primitive;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimitiveInjectionPointTest {

    @Test
    void testPrimitiveInjectionPointResolvedToNonPrimitiveProducerMethod() {
        Syringe syringe = newSyringe();
        try {
            Game game = getContextualReference(syringe, Game.class);
            assertTrue(game.getInjectedByte() == 0);
            assertTrue(game.getInjectedShort() == 0);
            assertTrue(game.getInjectedInt() == 0);
            assertTrue(game.getInjectedLong() == 0L);
            assertTrue(game.getInjectedFloat() == 0.0f);
            assertTrue(game.getInjectedDouble() == 0.0d);
            assertTrue(game.getInjectedChar() == '\u0000');
            assertFalse(game.isInjectedBoolean());
        } finally {
            syringe.shutdown();
        }
    }

    private static Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(ProducedPrimitive.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(PrimitiveProducer.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Game.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Bean<T> getBean(Syringe syringe, Class<T> type) {
        Set<Bean<?>> beans = syringe.getBeanManager().getBeans(type);
        return (Bean<T>) syringe.getBeanManager().resolve((Set) beans);
    }

    private static <T> T getContextualReference(Syringe syringe, Class<T> type) {
        Bean<T> bean = getBean(syringe, type);
        CreationalContext<T> creationalContext = syringe.getBeanManager().createCreationalContext(bean);
        return type.cast(syringe.getBeanManager().getReference(bean, type, creationalContext));
    }
}
