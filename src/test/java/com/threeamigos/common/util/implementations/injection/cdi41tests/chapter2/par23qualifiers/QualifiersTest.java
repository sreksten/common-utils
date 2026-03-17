package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter2.par23qualifiers;

import com.threeamigos.common.util.implementations.injection.util.QualifiersHelper;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Paragraph 2.3 - Qualifiers")
public class QualifiersTest {

    @Test
    @DisplayName("2.3.1 - Every bean has the built-in @Any qualifier")
    void everyBeanHasAnyQualifier() {
        Set<Annotation> orderQualifiers = QualifiersHelper.extractBeanQualifiers(Order.class.getAnnotations());
        Set<Annotation> synchronousOrderQualifiers = QualifiersHelper.extractBeanQualifiers(SynchronousOrder.class.getAnnotations());

        assertTrue(hasQualifier(orderQualifiers, Any.class), "Order must include @Any");
        assertTrue(hasQualifier(synchronousOrderQualifiers, Any.class), "SynchronousOrder must include @Any");
    }

    @Test
    @DisplayName("2.3.2 - Every bean not declaring @Named or @Any has the built-in @Default qualifier")
    void everyBeanNotDeclaringNamedOrAnyHasDefaultQualifier() {
        Set<Annotation> orderQualifiers = QualifiersHelper.extractBeanQualifiers(Order.class.getAnnotations());
        Set<Annotation> synchronousOrderQualifiers = QualifiersHelper.extractBeanQualifiers(SynchronousOrder.class.getAnnotations());

        assertTrue(hasQualifier(orderQualifiers, Default.class), "Order must include @Default");
        assertFalse(hasQualifier(synchronousOrderQualifiers, Default.class),
                "SynchronousOrder has an explicit qualifier and should not include @Default");
        assertTrue(hasQualifier(synchronousOrderQualifiers, Synchronous.class),
                "SynchronousOrder must keep its explicit @Synchronous qualifier");
    }

    private boolean hasQualifier(Set<Annotation> qualifiers, Class<? extends Annotation> qualifierType) {
        return qualifiers.stream().anyMatch(q -> q.annotationType().equals(qualifierType));
    }

}
