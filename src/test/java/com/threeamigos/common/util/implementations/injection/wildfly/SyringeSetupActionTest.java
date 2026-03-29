package com.threeamigos.common.util.implementations.injection.wildfly;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.CDIProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Isolated
class SyringeSetupActionTest {

    @Test
    void shouldConfigureCdiProviderAndExposeCurrentCdiDuringSetup() throws Exception {
        CdiStateSnapshot snapshot = CdiStateSnapshot.capture();
        Syringe syringe = null;
        try {
            setConfiguredProvider(null);
            setDiscoveredProviders(Collections.<CDIProvider>emptySet());
            assertThrows(IllegalStateException.class, CDI::current);

            syringe = new Syringe(new InMemoryMessageHandler());
            syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
            syringe.initialize();
            syringe.addDiscoveredClass(SetupActionBean.class, BeanArchiveMode.EXPLICIT);
            syringe.start();

            SyringeSetupAction action = new SyringeSetupAction(syringe);
            action.setup(Collections.<String, Object>emptyMap());
            try {
                SetupActionBean bean = CDI.current().select(SetupActionBean.class).get();
                assertNotNull(bean);
            } finally {
                action.teardown(Collections.<String, Object>emptyMap());
            }
        } finally {
            if (syringe != null) {
                syringe.shutdown();
            }
            snapshot.restore();
        }
    }

    private static void setDiscoveredProviders(Set<CDIProvider> providers) throws Exception {
        Field field = CDI.class.getDeclaredField("discoveredProviders");
        field.setAccessible(true);
        field.set(null, providers);
    }

    private static void setConfiguredProvider(CDIProvider provider) throws Exception {
        Field field = CDI.class.getDeclaredField("configuredProvider");
        field.setAccessible(true);
        field.set(null, provider);
    }

    private static final class CdiStateSnapshot {
        private final CDIProvider configuredProvider;
        private final Set<CDIProvider> discoveredProviders;

        private CdiStateSnapshot(CDIProvider configuredProvider, Set<CDIProvider> discoveredProviders) {
            this.configuredProvider = configuredProvider;
            this.discoveredProviders = discoveredProviders;
        }

        static CdiStateSnapshot capture() throws Exception {
            Field configuredField = CDI.class.getDeclaredField("configuredProvider");
            configuredField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Field discoveredField = CDI.class.getDeclaredField("discoveredProviders");
            discoveredField.setAccessible(true);
            return new CdiStateSnapshot((CDIProvider) configuredField.get(null), (Set<CDIProvider>) discoveredField.get(null));
        }

        void restore() throws Exception {
            setConfiguredProvider(configuredProvider);
            setDiscoveredProviders(discoveredProviders);
        }
    }

    @Dependent
    static class SetupActionBean {
    }
}
