package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.massOperations;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.Alternatives;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.Logger;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.Monitored;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.atd.TransactionLogger;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AfterTypeDiscoveryMassOperationsTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private AfterTypeBeanDiscoveryMassOperationObserver extension;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(new InMemoryMessageHandler(), AfterTypeDiscoveryMassOperationsTest.class);
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(AfterTypeBeanDiscoveryMassOperationObserver.class.getName());

        syringe.initialize();
        syringe.discover();
        syringe.addDiscoveredClass(TransactionLogger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Alternatives.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Logger.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Monitored.class, BeanArchiveMode.EXPLICIT);
        syringe.start();

        beanManager = syringe.getBeanManager();
        extension = AfterTypeBeanDiscoveryMassOperationObserver.getInstance();
        assertNotNull(extension);
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testInitialInterceptors() {
        assertTrue(extension.getInterceptors().contains(AlphaInterceptor.class));
        assertTrue(extension.getInterceptors().contains(BetaInterceptor.class));
        assertTrue(extension.getInterceptors().contains(GammaInterceptor.class));
    }

    @Test
    void testInitialAlternatives() {
        assertTrue(extension.getAlternatives().size() >= 3);
        List<Class<?>> alternatives = extension.getAlternatives();

        Integer alphaAltIndex = null;
        Integer betaAltIndex = null;
        Integer gammaAltIndex = null;

        for (int i = 0; i < alternatives.size(); i++) {
            if (alternatives.get(i).equals(AlphaAlternative.class)) {
                alphaAltIndex = i;
            }
            if (alternatives.get(i).equals(BetaAlternative.class)) {
                betaAltIndex = i;
            }
            if (alternatives.get(i).equals(GammaAlternative.class)) {
                gammaAltIndex = i;
            }
        }

        assertNotNull(alphaAltIndex);
        assertNotNull(betaAltIndex);
        assertNotNull(gammaAltIndex);
        assertTrue(alphaAltIndex < betaAltIndex);
        assertTrue(betaAltIndex < gammaAltIndex);
    }

    @Test
    void testInitialDecorators() {
        assertTrue(extension.getDecorators().size() >= 3);
        List<Class<?>> decorators = extension.getDecorators();

        Integer alphaDecIndex = null;
        Integer betaDecIndex = null;
        Integer gammaDecIndex = null;

        for (int i = 0; i < decorators.size(); i++) {
            if (decorators.get(i).equals(AlphaDecorator.class)) {
                alphaDecIndex = i;
            }
            if (decorators.get(i).equals(BetaDecorator.class)) {
                betaDecIndex = i;
            }
            if (decorators.get(i).equals(GammaDecorator.class)) {
                gammaDecIndex = i;
            }
        }

        assertNotNull(alphaDecIndex);
        assertNotNull(betaDecIndex);
        assertNotNull(gammaDecIndex);
        assertTrue(alphaDecIndex < betaDecIndex);
        assertTrue(betaDecIndex < gammaDecIndex);
    }

    @Test
    void testFinalInterceptors() {
        TransactionLogger logger = beanManager.createInstance().select(TransactionLogger.class).get();

        AlphaInterceptor.reset();
        BetaInterceptor.reset();
        GammaInterceptor.reset();

        logger.ping();

        assertTrue(AlphaInterceptor.isIntercepted());
        assertTrue(BetaInterceptor.isIntercepted());
        assertTrue(GammaInterceptor.isIntercepted());

        assertTrue(extension.containsWorks());
        assertTrue(extension.containsAllWorks());
    }

    @Test
    void testFinalDecorators() {
        TransactionLogger logger = beanManager.createInstance().select(TransactionLogger.class).get();
        assertEquals("pinggamma", logger.log("ping"));
    }

    @Test
    void testFinalAlternatives() {
        TransactionLogger logger = beanManager.createInstance().select(TransactionLogger.class).get();
        assertEquals(GammaAlternative.class, logger.getAlternativeClass());
        assertTrue(beanManager.getBeans(AlphaAlternative.class).isEmpty());
        assertTrue(beanManager.getBeans(BetaAlternative.class).isEmpty());
    }
}
