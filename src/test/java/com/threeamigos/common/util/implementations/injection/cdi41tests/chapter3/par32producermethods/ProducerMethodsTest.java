package com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods;

import com.threeamigos.common.util.implementations.injection.Syringe;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet1.InvalidAbstractProducerMethodBean;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet2.DependentNullProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet2.NonDependentNullProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet3.InvalidRawTypeArgumentProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet4.InvalidWildcardArrayProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet4.InvalidWildcardProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet5.DependentTypeVariableProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet5.NonDependentTypeVariableProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet6.InvalidTypeVariableProducerFactory;
import com.threeamigos.common.util.implementations.injection.cdi41tests.chapter3.par32producermethods.bullet7.InvalidTypeVariableArrayProducerFactory;
import com.threeamigos.common.util.implementations.injection.resolution.ProducerBean;
import com.threeamigos.common.util.implementations.messagehandler.InMemoryMessageHandler;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.spi.DefinitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("3.2 - Producer methods")
public class ProducerMethodsTest {

    /**
     * A producer method must be a default-access, public, protected or private,
     * non-abstract method of a managed bean class. A producer method may be either
     * static or non-static.
     */
    @Test
    @DisplayName("3.2 - Abstract producer method is a definition error")
    void abstractProducerMethodIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidAbstractProducerMethodBean.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    /**
     * If a producer method returns a null value at runtime and the producer method declares
     * any scope other than @Dependent, an IllegalProductException must be thrown.
     */
    @Test
    @DisplayName("3.2 - Non-@Dependent producer method returning null throws IllegalProductException")
    void nonDependentProducerMethodReturningNullThrowsIllegalProductException() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), NonDependentNullProducerFactory.class);
        syringe.setup();

        ProducerBean<?> producerBean = findProducerBeanByDeclaringClass(syringe, NonDependentNullProducerFactory.class);
        CreationalContext<?> creationalContext = syringe.getBeanManager().createCreationalContext(producerBean);

        assertThrows(IllegalProductException.class,
                () -> producerBean.create((CreationalContext) creationalContext));
    }

    /**
     * If a producer method can return null, @Dependent scope allows the product to be null.
     */
    @Test
    @DisplayName("3.2 - @Dependent producer method may return null")
    void dependentProducerMethodReturningNullIsAllowed() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), DependentNullProducerFactory.class);
        syringe.setup();

        ProducerBean<?> producerBean = findProducerBeanByDeclaringClass(syringe, DependentNullProducerFactory.class);
        CreationalContext<?> creationalContext = syringe.getBeanManager().createCreationalContext(producerBean);

        Object produced = producerBean.create((CreationalContext) creationalContext);
        assertNull(produced);
    }

    /**
     * If a producer method returns a parameterized type, each type parameter must be specified
     * with an actual type argument or type variable.
     */
    @Test
    @DisplayName("3.2 - Producer method with raw generic type argument is a definition error")
    void producerMethodWithRawGenericTypeArgumentIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidRawTypeArgumentProducerFactory.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    /**
     * If a producer method return type contains a wildcard type parameter,
     * the container must treat it as a definition error.
     */
    @Test
    @DisplayName("3.2 - Producer method with wildcard type parameter is a definition error")
    void producerMethodWithWildcardTypeParameterIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidWildcardProducerFactory.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    /**
     * If a producer method return type is an array type whose component type contains
     * a wildcard type parameter, the container must treat it as a definition error.
     */
    @Test
    @DisplayName("3.2 - Producer method with wildcard type parameter in array component is a definition error")
    void producerMethodWithWildcardTypeParameterInArrayComponentIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidWildcardArrayProducerFactory.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    /**
     * If a producer method return type is a parameterized type with a type variable,
     * the producer method is valid when its scope is @Dependent.
     */
    @Test
    @DisplayName("3.2 - Producer method with parameterized type variable return type is valid for @Dependent scope")
    void producerMethodWithTypeVariableReturnTypeIsValidForDependentScope() {
        InMemoryMessageHandler messageHandler = new InMemoryMessageHandler();
        Syringe syringe = new Syringe(messageHandler, DependentTypeVariableProducerFactory.class);
        syringe.exclude(NonDependentTypeVariableProducerFactory.class);

        assertDoesNotThrow(syringe::setup, () ->
                "Unexpected deployment errors: " + String.join(" | ", messageHandler.getAllErrorMessages()));
    }

    /**
     * If a producer method return type is a parameterized type with a type variable and
     * declares any scope other than @Dependent, the container must treat it as a definition error.
     */
    @Test
    @DisplayName("3.2 - Producer method with parameterized type variable return type is a definition error for non-@Dependent scope")
    void producerMethodWithTypeVariableReturnTypeIsDefinitionErrorForNonDependentScope() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), NonDependentTypeVariableProducerFactory.class);
        syringe.exclude(DependentTypeVariableProducerFactory.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    /**
     * If a producer method return type is a type variable,
     * the container must detect it and treat it as a definition error.
     */
    @Test
    @DisplayName("3.2 - Producer method with type variable return type is a definition error")
    void producerMethodWithTypeVariableReturnTypeIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidTypeVariableProducerFactory.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    /**
     * If a producer method return type is an array whose component type is a type variable,
     * the container must detect it and treat it as a definition error.
     */
    @Test
    @DisplayName("3.2 - Producer method with array component type variable return type is a definition error")
    void producerMethodWithArrayComponentTypeVariableReturnTypeIsDefinitionError() {
        Syringe syringe = new Syringe(new InMemoryMessageHandler(), InvalidTypeVariableArrayProducerFactory.class);

        assertThrows(DefinitionException.class, syringe::setup);
    }

    private ProducerBean<?> findProducerBeanByDeclaringClass(Syringe syringe, Class<?> declaringClass) {
        return syringe.getKnowledgeBase().getProducerBeans().stream()
                .filter(producerBean -> producerBean.getDeclaringClass().equals(declaringClass))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Producer bean not found for class: " + declaringClass.getName()));
    }

}
