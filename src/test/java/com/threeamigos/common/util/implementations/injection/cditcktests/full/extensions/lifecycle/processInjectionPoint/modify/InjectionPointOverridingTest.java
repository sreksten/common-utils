package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.lifecycle.processInjectionPoint.modify;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.injection.spi.BeanManagerImpl;
import jakarta.enterprise.util.AnnotationLiteral;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class InjectionPointOverridingTest {

    private static final String SESSION_ID = "process-injection-point-overriding-session";

    private Syringe syringe;

    @BeforeEach
    void setUp() {
        syringe = new Syringe();
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ModifyingExtension.class.getName());
        syringe.initialize();
        syringe.addDiscoveredClass(Dog.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(Hound.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(InjectingBean.class, BeanArchiveMode.EXPLICIT);
        syringe.addDiscoveredClass(AnimalDecorator.class, BeanArchiveMode.EXPLICIT);
        addBeansXmlConfiguration(syringe);
        syringe.start();
        BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
        beanManager.getContextManager().activateSession(SESSION_ID);
    }

    @AfterEach
    void tearDown() {
        if (syringe != null) {
            try {
                BeanManagerImpl beanManager = (BeanManagerImpl) syringe.getBeanManager();
                beanManager.getContextManager().deactivateSession();
            } catch (Exception ignored) {
                // Session might not be active if startup failed.
            }
            syringe.shutdown();
        }
    }

    @Test
    void testOverridingFieldInjectionPoint() {
        InjectingBean bean = syringe.getBeanManager().createInstance().select(InjectingBean.class).get();
        assertTrue(bean.getDog() instanceof Hound);
    }

    @Test
    void testDelegateInjectionPoint() {
        Hound hound = syringe.getBeanManager().createInstance().select(Hound.class, Fast.Literal.INSTANCE).get();
        Dog dog = syringe.getBeanManager().createInstance().select(Dog.class, LazyLiteral.INSTANCE).get();

        assertNotNull(hound);
        assertTrue(hound.decorated());
        assertNotNull(dog);
        assertTrue(dog.decorated());
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<decorators>" +
                "<class>" + AnimalDecorator.class.getName() + "</class>" +
                "</decorators>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    private static final class LazyLiteral extends AnnotationLiteral<Lazy> implements Lazy {
        private static final LazyLiteral INSTANCE = new LazyLiteral();
        private static final long serialVersionUID = 1L;
    }
}
