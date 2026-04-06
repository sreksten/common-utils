package com.threeamigos.common.util.implementations.injection.cditcktests.se.customCDIProvider;

import com.threeamigos.common.util.implementations.injection.spi.SyringeCDIProvider;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.CDIProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class CustomCDIProviderTest {

    private CdiStateSnapshot snapshot;

    @BeforeEach
    void setUp() throws Exception {
        snapshot = CdiStateSnapshot.capture();
        resetCdiProviderState();
        CustomCDIProvider.initializedCalled = false;
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            snapshot.restore();
        } finally {
            SyringeCDIProvider.ensureProviderConfigured();
        }
    }

    @Test
    void testCustomCDIProvider() {
        CDI.setCDIProvider(new CustomCDIProvider());
        CDI<Object> current = CDI.current();
        assertNotNull(current);
        assertTrue(CustomCDIProvider.initializedCalled);
    }

    private void resetCdiProviderState() throws Exception {
        Field providerSetManuallyField = CDI.class.getDeclaredField("providerSetManually");
        Field discoveredProvidersField = CDI.class.getDeclaredField("discoveredProviders");
        Field configuredProviderField = CDI.class.getDeclaredField("configuredProvider");
        providerSetManuallyField.setAccessible(true);
        discoveredProvidersField.setAccessible(true);
        configuredProviderField.setAccessible(true);
        providerSetManuallyField.setBoolean(null, false);
        discoveredProvidersField.set(null, null);
        configuredProviderField.set(null, null);
    }

    private static final class CdiStateSnapshot {
        private final boolean providerSetManually;
        private final Set<CDIProvider> discoveredProviders;
        private final CDIProvider configuredProvider;

        private CdiStateSnapshot(boolean providerSetManually, Set<CDIProvider> discoveredProviders, CDIProvider configuredProvider) {
            this.providerSetManually = providerSetManually;
            this.discoveredProviders = discoveredProviders;
            this.configuredProvider = configuredProvider;
        }

        private static CdiStateSnapshot capture() throws Exception {
            Field providerSetManuallyField = CDI.class.getDeclaredField("providerSetManually");
            Field discoveredProvidersField = CDI.class.getDeclaredField("discoveredProviders");
            Field configuredProviderField = CDI.class.getDeclaredField("configuredProvider");
            providerSetManuallyField.setAccessible(true);
            discoveredProvidersField.setAccessible(true);
            configuredProviderField.setAccessible(true);
            boolean providerSetManually = providerSetManuallyField.getBoolean(null);
            @SuppressWarnings("unchecked")
            Set<CDIProvider> discoveredProviders = (Set<CDIProvider>) discoveredProvidersField.get(null);
            CDIProvider configuredProvider = (CDIProvider) configuredProviderField.get(null);
            return new CdiStateSnapshot(providerSetManually, discoveredProviders, configuredProvider);
        }

        private void restore() throws Exception {
            Field providerSetManuallyField = CDI.class.getDeclaredField("providerSetManually");
            Field discoveredProvidersField = CDI.class.getDeclaredField("discoveredProviders");
            Field configuredProviderField = CDI.class.getDeclaredField("configuredProvider");
            providerSetManuallyField.setAccessible(true);
            discoveredProvidersField.setAccessible(true);
            configuredProviderField.setAccessible(true);
            providerSetManuallyField.setBoolean(null, providerSetManually);
            discoveredProvidersField.set(null, discoveredProviders);
            configuredProviderField.set(null, configuredProvider);
        }
    }
}
