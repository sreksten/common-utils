package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.Alpha.AlphaLiteral;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.Bravo.BravoLiteral;
import com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.afterBeanDiscovery.annotated.Charlie.CharlieLiteral;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.literal.InjectLiteral;
import jakarta.enterprise.inject.spi.AnnotatedType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAnnotatedTypesTest {

    private Syringe syringe;
    private ModifyingExtension extension;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Foo.class,
                Bar.class,
                Alpha.class,
                Bravo.class,
                Charlie.class,
                ModifyingExtension.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ModifyingExtension.class.getName());
        syringe.setup();
        extension = syringe.getBeanManager().getExtension(ModifyingExtension.class);
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testGetAnnotatedType() {
        AnnotatedType<Foo> aplha = extension.getAplha();
        assertNotNull(aplha);
        assertEquals(1, aplha.getAnnotations().size());
        assertEquals(AlphaLiteral.INSTANCE, aplha.getAnnotations().iterator().next());
        assertEquals(1, aplha.getMethods().size());
        assertEquals(1, aplha.getMethods().iterator().next().getAnnotations().size());
        assertEquals(InjectLiteral.INSTANCE, aplha.getMethods().iterator().next().getAnnotations().iterator().next());

        AnnotatedType<Foo> bravo = extension.getBravo();
        assertNotNull(bravo);
        assertEquals(2, bravo.getAnnotations().size());
        assertTrue(bravo.getAnnotations().contains(BravoLiteral.INSTANCE));
        assertTrue(bravo.getAnnotations().contains(Any.Literal.INSTANCE));

        AnnotatedType<Foo> charlie = extension.getCharlie();
        assertNotNull(charlie);
        assertEquals(1, charlie.getAnnotations().size());
        assertEquals(CharlieLiteral.INSTANCE, charlie.getAnnotations().iterator().next());

        AnnotatedType<Bar> bar = extension.getBar();
        assertNotNull(bar);
        assertEquals(2, bar.getAnnotations().size());
    }

    @Test
    void testGetAnnotatedTypes() {
        List<AnnotatedType<Foo>> allFoo = extension.getAllFoo();
        assertEquals(3, allFoo.size());
        assertTrue(allFoo.contains(extension.getAplha()));
        assertTrue(allFoo.contains(extension.getBravo()));
        assertTrue(allFoo.contains(extension.getCharlie()));
    }
}
