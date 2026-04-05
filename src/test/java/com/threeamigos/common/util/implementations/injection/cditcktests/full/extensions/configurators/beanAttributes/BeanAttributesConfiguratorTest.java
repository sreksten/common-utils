package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.configurators.beanAttributes;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeanAttributesConfiguratorTest {

    static final String SWORD_NAME = "Frostmourne";

    private Syringe syringe;
    private BeanManager beanManager;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Axe.class,
                Equipment.class,
                Hoe.class,
                Mace.class,
                Melee.class,
                ProcessBeanAttributesObserver.class,
                Reforged.class,
                Sword.class,
                Tool.class,
                TwoHanded.class,
                UsableItem.class,
                Weapon.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        addAllDiscoveryBeansXmlWithAlternative(syringe, Axe.class);
        syringe.addExtension(ProcessBeanAttributesObserver.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testSingleAdditionMethods() {
        Bean<Sword> bean = getUniqueBean(Sword.class, TwoHanded.TwoHandedLiteral.INSTANCE);

        assertTrue(bean.getQualifiers().contains(TwoHanded.TwoHandedLiteral.INSTANCE));
        assertEquals(SWORD_NAME, bean.getName());
        assertTrue(bean.getTypes().contains(Weapon.class));
        assertTrue(bean.getStereotypes().contains(Equipment.class));
    }

    @Test
    void testMultiAdditionMethods() {
        Bean<Axe> bean = getUniqueBean(Axe.class, TwoHanded.TwoHandedLiteral.INSTANCE, Reforged.ReforgedLiteral.INSTANCE);

        Set<Annotation> qualifiers = bean.getQualifiers();
        Set<Class<? extends Annotation>> stereotypes = bean.getStereotypes();
        Set<Type> types = bean.getTypes();

        assertEquals(RequestScoped.class, bean.getScope());
        assertTrue(qualifiers.containsAll(
                new HashSet<Annotation>(Arrays.asList(Reforged.ReforgedLiteral.INSTANCE, TwoHanded.TwoHandedLiteral.INSTANCE))));
        assertTrue(stereotypes.containsAll(new HashSet<Class<? extends Annotation>>(Arrays.asList(Melee.class, Equipment.class))));
        assertTrue(types.containsAll(new HashSet<Type>(Arrays.asList(Weapon.class, Tool.class))));
        assertTrue(bean.isAlternative());
    }

    @Test
    void testReplacementMethods() {
        Bean<Hoe> bean = getUniqueBean(Hoe.class, Reforged.ReforgedLiteral.INSTANCE);

        Set<Type> types = bean.getTypes();
        Set<Class<? extends Annotation>> stereotypes = bean.getStereotypes();

        assertEquals(new HashSet<Annotation>(Arrays.asList(Reforged.ReforgedLiteral.INSTANCE, Any.Literal.INSTANCE)),
                bean.getQualifiers());
        assertTrue(types.containsAll(new HashSet<Type>(Arrays.asList(Tool.class, UsableItem.class))));
        assertEquals(new HashSet<Class<? extends Annotation>>(Arrays.asList(Equipment.class)), stereotypes);
    }

    @Test
    void configuratorInitializedWithOriginalBeanAttributes() {
        Bean<Mace> configuredBean = getUniqueBean(Mace.class);
        BeanAttributes<Mace> originalBA = beanManager.getExtension(ProcessBeanAttributesObserver.class).getOriginalBA();
        assertEquals(configuredBean.getTypes(), originalBA.getTypes());
        assertEquals(configuredBean.getQualifiers(), originalBA.getQualifiers());
        assertEquals(configuredBean.getStereotypes(), originalBA.getStereotypes());
        assertEquals(configuredBean.getScope(), originalBA.getScope());
    }

    private static void addAllDiscoveryBeansXmlWithAlternative(Syringe syringe, Class<?> alternativeClass) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" " +
                "version=\"3.0\" bean-discovery-mode=\"all\">" +
                "<alternatives><class>" + alternativeClass.getName() + "</class></alternatives>" +
                "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Bean<T> getUniqueBean(Class<T> beanClass, Annotation... qualifiers) {
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        return (Bean<T>) beanManager.resolve((Set) beans);
    }
}
