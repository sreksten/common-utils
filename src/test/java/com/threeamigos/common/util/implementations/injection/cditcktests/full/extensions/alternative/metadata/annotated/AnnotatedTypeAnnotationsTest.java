package com.threeamigos.common.util.implementations.injection.cditcktests.full.extensions.alternative.metadata.annotated;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.discovery.BeanArchiveMode;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnnotatedTypeAnnotationsTest {

    private Syringe syringe;
    private BeanManager beanManager;
    private ObservingExtension extension;

    @BeforeAll
    void setUp() {
        syringe = new Syringe(
                new InMemoryMessageHandler(),
                Android.class,
                Being.class,
                Fate.class,
                Human.class,
                InheritedQualifier.class,
                Kryten.class,
                Mortal.class,
                NotInheritedQualifier.class,
                NotInheritedStereotype.class,
                ObservingExtension.class,
                Rimmer.class
        );
        syringe.forceBeanArchiveMode(BeanArchiveMode.EXPLICIT);
        syringe.addExtension(ObservingExtension.class.getName());
        syringe.setup();
        beanManager = syringe.getBeanManager();
        extension = beanManager.getExtension(ObservingExtension.class);
    }

    @AfterAll
    void tearDown() {
        if (syringe != null) {
            syringe.shutdown();
        }
    }

    @Test
    void testCreateAnnotatedType() {
        assertAnnotationSetMatches(beanManager.createAnnotatedType(Android.class).getAnnotations(),
                RequestScoped.class, InheritedQualifier.class, Fate.class);
        assertAnnotationSetMatches(beanManager.createAnnotatedType(Rimmer.class).getAnnotations(),
                Mortal.class, Dependent.class, InheritedQualifier.class, Fate.class);
    }

    @Test
    void testProcessAnnotatedType() {
        AnnotatedType<Kryten> kryten = extension.getKryten();
        assertNotNull(kryten);
        assertAnnotationSetMatches(kryten.getAnnotations(), RequestScoped.class, InheritedQualifier.class, Fate.class);

        AnnotatedType<Rimmer> rimmer = extension.getRimmer();
        assertNotNull(rimmer);
        assertAnnotationSetMatches(rimmer.getAnnotations(),
                Mortal.class, Dependent.class, InheritedQualifier.class, Fate.class);
    }

    @Test
    void testGetAnnotatedType() {
        AnnotatedType<Android> android = extension.getAndroid();
        assertNotNull(android);
        assertAnnotationSetMatches(android.getAnnotations(), RequestScoped.class, InheritedQualifier.class, Fate.class);
    }

    @Test
    void testGetAnnotatedTypes() {
        List<AnnotatedType<Human>> humans = extension.getAllHumans();
        assertNotNull(humans);
        assertEquals(1, humans.size());
        assertAnnotationSetMatches(humans.iterator().next().getAnnotations(),
                Mortal.class, Dependent.class, InheritedQualifier.class, Fate.class);

        List<AnnotatedType<Android>> androids = extension.getAllAndroids();
        assertNotNull(androids);
        assertEquals(1, androids.size());
        assertAnnotationSetMatches(androids.iterator().next().getAnnotations(),
                RequestScoped.class, InheritedQualifier.class, Fate.class);
    }

    @SafeVarargs
    private static void assertAnnotationSetMatches(Set<? extends Annotation> annotations,
                                                   Class<? extends Annotation>... requiredAnnotationTypes) {
        assertNotNull(annotations);
        assertEquals(requiredAnnotationTypes.length, annotations.size(),
                "Unexpected annotation set size: " + annotations);
        for (Class<? extends Annotation> requiredAnnotationType : requiredAnnotationTypes) {
            boolean present = false;
            for (Annotation annotation : annotations) {
                if (requiredAnnotationType.equals(annotation.annotationType())) {
                    present = true;
                    break;
                }
            }
            assertTrue(present, "Missing annotation: " + requiredAnnotationType.getName());
        }
    }
}
