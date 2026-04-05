package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.processBean;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXml;
import com.threeamigos.common.util.implementations.injection.beansxml.BeansXmlParser;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.inject.spi.ProcessProducerField;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Isolated
class ProcessBeanTest {

    private Syringe syringe;

    @BeforeAll
    void setUp() {
        ProcessBeanObserver.reset();

        syringe = new Syringe(
                new InMemoryMessageHandler(),
                ProcessBeanObserver.class,
                Cat.class,
                Cow.class,
                Cowshed.class,
                Domestic.class,
                Chicken.class,
                ChickenHutch.class,
                CatInterceptor.class,
                CatInterceptorBinding.class,
                Animal.class,
                AnimalDecorator.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ProcessBeanObserver.class.getName());
        addBeansXmlConfiguration(syringe);
        syringe.setup();
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testProcessBeanEvent() {
        assertNotNull(ProcessBeanObserver.getCatBean());
        assertEquals(Cat.class, ProcessBeanObserver.getCatBean().getBeanClass());
        assertTrue(annotationSetMatches(ProcessBeanObserver.getCatBean().getQualifiers(), Domestic.class, Any.class));
        assertEquals(Cat.class, ProcessBeanObserver.getCatAnnotatedType().getBaseType());
        assertEquals(2, ProcessBeanObserver.getCatProcessBeanCount());
        assertTrue(ProcessBeanObserver.getCatAnnotated() instanceof AnnotatedType<?>);

        assertEquals(
                Arrays.asList(ProcessBeanAttributes.class.getName(), ProcessManagedBean.class.getName()),
                ProcessBeanObserver.getCatActionSeq().getData()
        );
    }

    @Test
    void testProcessProducerMethodEvent() {
        assertTrue(ProcessBeanObserver.getCowBean().getTypes().contains(Cow.class));
        assertEquals(Cowshed.class, ProcessBeanObserver.getCowBean().getBeanClass());
        assertEquals(Cow.class, ProcessBeanObserver.getCowMethod().getBaseType());
        assertEquals(Cowshed.class, ProcessBeanObserver.getCowMethod().getDeclaringType().getBaseType());

        assertEquals(2, ProcessBeanObserver.getCowShedProcessBeanCount());
        assertTrue(ProcessBeanObserver.getCowAnnotated() instanceof AnnotatedMethod<?>);

        assertEquals("getDaisy", ProcessBeanObserver.getCowMethod().getJavaMember().getName());
        assertEquals(Cowshed.class, ProcessBeanObserver.getCowMethod().getJavaMember().getDeclaringClass());

        AnnotatedParameter<Cow> disposedParam = ProcessBeanObserver.getCowParameter();
        assertNotNull(disposedParam);
        assertTrue(disposedParam.isAnnotationPresent(Disposes.class));
        assertEquals(Cow.class, disposedParam.getBaseType());
        assertEquals("disposeOfDaisy", disposedParam.getDeclaringCallable().getJavaMember().getName());
        assertEquals(Cowshed.class, disposedParam.getDeclaringCallable().getJavaMember().getDeclaringClass());
        assertEquals(Cowshed.class, disposedParam.getDeclaringCallable().getDeclaringType().getJavaClass());

        assertEquals(
                Arrays.asList(ProcessBeanAttributes.class.getName(), ProcessProducerMethod.class.getName()),
                ProcessBeanObserver.getCowActionSeq().getData()
        );
    }

    @Test
    void testProcessProducerFieldEvent() {
        assertTrue(ProcessBeanObserver.getChickenBean().getTypes().contains(Chicken.class));
        assertEquals(ChickenHutch.class, ProcessBeanObserver.getChickenBean().getBeanClass());
        assertEquals(Chicken.class, ProcessBeanObserver.getChickenField().getBaseType());
        assertEquals(ChickenHutch.class, ProcessBeanObserver.getChickenField().getDeclaringType().getBaseType());

        assertEquals(2, ProcessBeanObserver.getChickenHutchProcessBeanCount());
        assertTrue(ProcessBeanObserver.getChickedAnnotated() instanceof AnnotatedField<?>);

        assertEquals("chicken", ProcessBeanObserver.getChickenField().getJavaMember().getName());
        assertEquals(ChickenHutch.class, ProcessBeanObserver.getChickenField().getJavaMember().getDeclaringClass());

        AnnotatedParameter<Chicken> disposedParam = ProcessBeanObserver.getChickenParameter();
        assertNotNull(disposedParam);
        assertTrue(disposedParam.isAnnotationPresent(Disposes.class));
        assertEquals(Chicken.class, disposedParam.getBaseType());
        assertEquals("disposeOfRocky", disposedParam.getDeclaringCallable().getJavaMember().getName());
        assertEquals(ChickenHutch.class, disposedParam.getDeclaringCallable().getJavaMember().getDeclaringClass());
        assertEquals(ChickenHutch.class, disposedParam.getDeclaringCallable().getDeclaringType().getJavaClass());

        assertEquals(
                Arrays.asList(ProcessBeanAttributes.class.getName(), ProcessProducerField.class.getName()),
                ProcessBeanObserver.getChickenActionSeq().getData()
        );
    }

    @Test
    void testProcessBeanFiredForInterceptor() {
        assertNotNull(ProcessBeanObserver.getInterceptor());
    }

    @Test
    void testProcessBeanFiredForDecorator() {
        assertNotNull(ProcessBeanObserver.getDecorator());
    }

    @SafeVarargs
    private final boolean annotationSetMatches(Set<? extends Annotation> actual, Class<? extends Annotation>... expected) {
        Set<Class<? extends Annotation>> actualTypes = new HashSet<Class<? extends Annotation>>();
        for (Annotation annotation : actual) {
            actualTypes.add(annotation.annotationType());
        }
        return actualTypes.equals(new HashSet<Class<? extends Annotation>>(Arrays.asList(expected)));
    }

    private static void addBeansXmlConfiguration(Syringe syringe) {
        String xml = "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\" "
                + "version=\"3.0\" bean-discovery-mode=\"all\">"
                + "<interceptors><class>" + CatInterceptor.class.getName() + "</class></interceptors>"
                + "<decorators><class>" + AnimalDecorator.class.getName() + "</class></decorators>"
                + "</beans>";
        BeansXml beansXml = new BeansXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        syringe.addBeansXmlConfiguration(beansXml);
    }
}
