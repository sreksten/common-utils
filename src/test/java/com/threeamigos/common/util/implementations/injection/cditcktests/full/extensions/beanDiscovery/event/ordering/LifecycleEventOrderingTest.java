package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.beanDiscovery.event.ordering;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LifecycleEventOrderingTest {

    private Syringe syringe;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                HastilyWritten.class,
                HighQualityAndLowCostProduct.class,
                MassiveJugCoffee.class,
                PoorWorker.class,
                ProductManagement.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProductManagement.class.getName());
        syringe.setup();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testEventsWereFiredInCorrectOrderForProducer() {
        ActionSequence producerEventsSeq = ActionSequence.getSequence(ProductManagement.PRODUCER_SEQ);
        assertNotNull(producerEventsSeq);
        producerEventsSeq.assertDataEquals("PIP", "PP", "PBA", "PPM");
    }

    @Test
    void testEventsWereFiredInCorrectOrderForManagedBean() {
        ActionSequence producerEventsSeq = ActionSequence.getSequence(ProductManagement.BEAN_SEQ);
        assertNotNull(producerEventsSeq);
        producerEventsSeq.assertDataEquals("PIP", "PIT", "PBA", "PB");
    }
}
