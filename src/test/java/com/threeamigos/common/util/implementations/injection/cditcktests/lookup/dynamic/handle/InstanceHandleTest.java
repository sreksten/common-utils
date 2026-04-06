package com.threeamigos.common.util.implementations.injection.cditcktests.lookup.dynamic.handle;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class InstanceHandleTest {

    @Test
    void testIsResolvable() {
        Syringe syringe = newSyringe();
        try {
            Client client = syringe.getBeanManager().createInstance().select(Client.class).get();
            ActionSequence.reset();
            assertNotNull(client);
            assertTrue(client.getAlphaInstance().isResolvable());
            assertFalse(client.getBigDecimalInstance().isResolvable());
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetHandle() {
        Syringe syringe = newSyringe();
        try {
            Client client = syringe.getBeanManager().createInstance().select(Client.class).get();
            BeanManager beanManager = syringe.getBeanManager();

            ActionSequence.reset();
            assertNotNull(client);

            Bean<?> alphaBean = beanManager.resolve(beanManager.getBeans(Alpha.class));
            Instance<Alpha> instance = client.getAlphaInstance();

            Instance.Handle<Alpha> alpha1 = instance.getHandle();
            assertEquals(alphaBean, alpha1.getBean());
            assertEquals(Dependent.class, alpha1.getBean().getScope());

            String alpha2Id;
            try (Instance.Handle<Alpha> alpha2 = instance.getHandle()) {
                alpha2Id = alpha2.get().getId();
                assertFalse(alpha1.get().getId().equals(alpha2Id));
            }

            List<String> sequence = ActionSequence.getSequenceData();
            assertEquals(1, sequence.size());
            assertEquals(alpha2Id, sequence.get(0));

            alpha1.destroy();
            alpha1.destroy();

            sequence = ActionSequence.getSequenceData();
            assertEquals(2, sequence.size());

            Instance<Bravo> bravoInstance = client.getInstance().select(Bravo.class);
            String bravoId = bravoInstance.get().getId();
            try (Instance.Handle<Bravo> bravo = bravoInstance.getHandle()) {
                assertEquals(bravoId, bravo.get().getId());
                ActionSequence.reset();
            }
            sequence = ActionSequence.getSequenceData();
            assertEquals(1, sequence.size());
            assertEquals(bravoId, sequence.get(0));
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testGetAfterDestroyingContextualInstance() {
        Syringe syringe = newSyringe();
        try {
            ActionSequence.reset();
            Client client = syringe.getBeanManager().createInstance().select(Client.class).get();
            assertNotNull(client);

            Instance.Handle<Alpha> alphaHandle = client.getAlphaInstance().getHandle();
            alphaHandle.get();
            alphaHandle.destroy();

            List<String> sequence = ActionSequence.getSequenceData();
            assertEquals(1, sequence.size());

            assertThrows(IllegalStateException.class, alphaHandle::get);
        } finally {
            syringe.shutdown();
        }
    }

    @Test
    void testHandles() {
        Syringe syringe = newSyringe();
        try {
            Instance<Processor> instance = syringe.getBeanManager().createInstance().select(Processor.class);
            ActionSequence.reset();

            assertTrue(instance.isAmbiguous());
            for (Instance.Handle<Processor> handle : instance.handles()) {
                handle.get().ping();
                if (Dependent.class.equals(handle.getBean().getScope())) {
                    handle.destroy();
                }
            }

            assertEquals(3, ActionSequence.getSequenceSize());
            List<String> firstPass = ActionSequence.getSequenceData();
            assertTrue(firstPass.contains("firstPing"));
            assertTrue(firstPass.contains("secondPing"));
            assertTrue(firstPass.contains("firstDestroy"));

            ActionSequence.reset();
            assertTrue(instance.isAmbiguous());
            for (Iterator<? extends Instance.Handle<Processor>> iterator = instance.handles().iterator(); iterator.hasNext();) {
                try (Instance.Handle<Processor> handle = iterator.next()) {
                    handle.get().ping();
                }
            }

            assertEquals(4, ActionSequence.getSequenceSize());
            List<String> secondPass = ActionSequence.getSequenceData();
            assertTrue(secondPass.contains("firstPing"));
            assertTrue(secondPass.contains("secondPing"));
            assertTrue(secondPass.contains("firstDestroy"));
            assertTrue(secondPass.contains("secondDestroy"));
        } finally {
            syringe.shutdown();
        }
    }

    private Syringe newSyringe() {
        Syringe syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.initialize();
        syringe.addDiscoveredClass(Alpha.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Bravo.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Client.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(FirstProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Juicy.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Processor.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(SecondProcessor.class, BeanArchiveMode.EXPLICIT);
        syringe.start();
        return syringe;
    }

}
