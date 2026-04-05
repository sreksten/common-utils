package com.threeamigos.common.util.implementations.injection.cditcktests.full.event;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTest {

    @Test
    void testObserverCalledOnSpecializedBeanOnly() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), Delivery.class, Shop.class, FarmShop.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.setup();
        try {
            Shop.observers.clear();
            syringe.getBeanManager().getEvent().select(Delivery.class).fire(new Delivery());

            assertEquals(1, Shop.observers.size());
            assertEquals(FarmShop.class.getName(), Shop.observers.iterator().next());
        } finally {
            syringe.shutdown();
            Shop.observers.clear();
        }
    }
}
