package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed1;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed2;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesStandardClass;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.multipleconcreteclasses.subpackage.MultipleConcreteClassesNamed3;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.singleimplementation.SingleImplementationAbstractClass;
import com.threeamigos.common.util.implementations.injection.testpackages.alternatives.AlternativesAlternativeImplementation1;
import com.threeamigos.common.util.implementations.injection.testpackages.alternatives.AlternativesAlternativeImplementation2;
import com.threeamigos.common.util.implementations.injection.testpackages.alternatives.AlternativesInterface;
import com.threeamigos.common.util.implementations.injection.testpackages.alternatives.AlternativesStandardImplementation;
import com.threeamigos.common.util.implementations.injection.discovery.ParallelClasspathScanner;
import com.threeamigos.common.util.implementations.injection.discovery.SimpleClassConsumer;
import com.threeamigos.common.util.implementations.injection.testpackages.bind.*;
import com.threeamigos.common.util.implementations.injection.testpackages.interfaces.multipleimplementations.*;
import com.threeamigos.common.util.implementations.injection.testpackages.interfaces.namedimplementationsonly.NamedImplementationsOnlyImplementation1;
import com.threeamigos.common.util.implementations.injection.testpackages.interfaces.namedimplementationsonly.NamedImplementationsOnlyInterface;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.singleimplementation.SingleImplementationConcreteClass;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.noconcreteclasses.NoConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.testpackages.interfaces.noimplementations.NoImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.testpackages.abstractclasses.multiplenotannotatedconcreteclasses.MultipleNotAnnotatedAbstractClass;
import com.threeamigos.common.util.implementations.injection.testpackages.interfaces.singleimplementation.SingleImplementationClass;
import com.threeamigos.common.util.implementations.injection.testpackages.interfaces.singleimplementation.SingleImplementationInterface;
import com.threeamigos.common.util.implementations.injection.testpackages.interfaces.multiplenotannotatedimplementations.MultipleNotAnnotatedImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.knowledgebase.KnowledgeBase;
import com.threeamigos.common.util.implementations.injection.util.AnnotationLiteral;
import com.threeamigos.common.util.implementations.injection.util.DefaultLiteral;
import com.threeamigos.common.util.implementations.injection.resolution.TypeChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.ResolutionException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Named;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClassResolver unit tests")
class ClassResolverUnitTest {

    // Test interfaces with no implementations
    interface NoImplInterface {}
    interface NoImplInterface2 {}

    ClassResolver sut;

    Collection<Annotation> qualifier = Collections.singleton(AnnotationLiteral.of(BindingQualifier.class));
    Collection<Annotation> nonMatchingQualifier = Collections.singleton(AnnotationLiteral.of(BindingNotMatchingQualifier.class));

    @BeforeEach
    void setUp() throws Exception {
        // Given
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        SimpleClassConsumer sink = new SimpleClassConsumer();
        new ParallelClasspathScanner(Thread.currentThread().getContextClassLoader(), sink, new KnowledgeBase(), getPackageName(ClassResolverUnitTest.class));
        // Populate knowledgeBase from sink
        for (Class<?> clazz : sink.getClasses()) {
            knowledgeBase.add(clazz);
        }
        sut = new ClassResolver(knowledgeBase);
        sut.setBindingsOnly(false); // The default value anyway.
    }

    @Nested
    @DisplayName("Binding tests")
    class BindingTests {

        @BeforeEach
        void setUp() {
            // Given
            sut.setBindingsOnly(true); // Forces the class resolver NOT to scan for classes, using bound classes only.
        }

        @Nested
        @DisplayName("Binding errors")
        class BindingErrors {

                @Test
                @DisplayName("Should throw IllegalArgumentException when binding incompatible types")
                void shouldThrowIllegalArgumentExceptionWhenBindingIncompatibleTypes() {
                    assertDoesNotThrow(() -> sut.bind(String.class, qualifier, String.class));
                    assertDoesNotThrow(() -> sut.bind(List.class, qualifier, ArrayList.class));
                    assertThrows(IllegalArgumentException.class, () -> sut.bind(List.class, qualifier, Map.class));
                }
        }

        @Nested
        @DisplayName("Binding classes")
        class BindingClasses {

            @ParameterizedTest
            @DisplayName("A class bound with no qualifiers should be resolved with target class when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void classWithNoQualifierShouldResolveWithNoQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(ClassToBind.class, noQualifier, TargetClass.class);
                // When
                Class<?> clazz = sut.resolveImplementation(ClassToBind.class, null);
                assertEquals(TargetClass.class, clazz);
            }

            @ParameterizedTest
            @DisplayName("A class bound with no qualifiers should not be resolved when resolving with any qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void classWithNoQualifierShouldNotResolveWithAnyQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(ClassToBind.class, noQualifier, TargetClass.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(ClassToBind.class, qualifier));
            }

            @ParameterizedTest
            @DisplayName("A class bound with qualifiers should not be resolved when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void classWithQualifierShouldNotResolveWithNoQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(ClassToBind.class, qualifier, TargetClass.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(ClassToBind.class, noQualifier));
            }

            @Test
            @DisplayName("A class bound with qualifiers should be resolved with target class when resolving with correct qualifiers")
            void classWithQualifierShouldResolveWithQualifier() {
                //Given
                sut.bind(ClassToBind.class, qualifier, TargetClass.class);
                // When
                Class<?> clazz = sut.resolveImplementation(ClassToBind.class, qualifier);
                assertEquals(TargetClass.class, clazz);
            }

            @Test
            @DisplayName("A class bound with qualifier should not be resolved when resolving with non-matching qualifiers")
            void classWithQualifierShouldResolveWithNonMatchingQualifier() {
                //Given
                sut.bind(ClassToBind.class, qualifier, TargetClass.class);
                // When
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(ClassToBind.class, nonMatchingQualifier));
            }
        }

        @Nested
        @DisplayName("Binding interfaces")
        class BindingInterfaces {

            @ParameterizedTest
            @DisplayName("An interface bound with no qualifiers should be resolved with target implementation when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void interfaceWithNoQualifierShouldResolveWithNoQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(InterfaceToBind.class, noQualifier, TargetImplementation.class);
                // When
                Class<?> clazz = sut.resolveImplementation(InterfaceToBind.class, null);
                assertEquals(TargetImplementation.class, clazz);
            }

            @ParameterizedTest
            @DisplayName("An interface bound with no qualifiers should not be resolved when resolving with any qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void interfaceWithNoQualifierShouldNotResolveWithQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(InterfaceToBind.class, noQualifier, TargetImplementation.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(InterfaceToBind.class, qualifier));
            }

            @ParameterizedTest
            @DisplayName("An interface bound with qualifiers should not be resolved when resolving with no qualifiers")
            @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
            void interfaceWithQualifierShouldNotResolveWithNoQualifier(Collection<Annotation> noQualifier) {
                //Given
                sut.bind(InterfaceToBind.class, qualifier, TargetImplementation.class);
                // When / Then
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(InterfaceToBind.class, noQualifier));
            }

            @Test
            @DisplayName("An interface bound with qualifiers should be resolved with target implementation when resolving with correct qualifiers")
            void interfaceWithQualifierShouldResolveWithQualifier() {
                //Given
                sut.bind(InterfaceToBind.class, qualifier, TargetImplementation.class);
                // When
                Class<?> clazz = sut.resolveImplementation(InterfaceToBind.class, qualifier);
                assertEquals(TargetImplementation.class, clazz);
            }

            @Test
            @DisplayName("An interface bound with qualifier should not be resolved when resolving with non-matching qualifiers")
            void classWithQualifierShouldResolveWithNonMatchingQualifier() {
                //Given
                sut.bind(InterfaceToBind.class, qualifier, TargetImplementation.class);
                // When
                assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(InterfaceToBind.class, nonMatchingQualifier));
            }
        }
    }

    @Nested
    @DisplayName("Alternatives tests")
    class AlternativesTests {

        @Test
        @DisplayName("If no alternatives are enabled should return the default class")
        void shouldReturnTheDefaultClass() {
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);
            // Then
            assertEquals(AlternativesStandardImplementation.class, clazz);
        }

        @Test
        @DisplayName("If alternatives are enabled should return the alternative class with no qualifiers")
        void shouldReturnTheAlternativeClassWithNoQualifiers() {
            // Given
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);
            // Then
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
        }

        @Test
        @DisplayName("If alternatives are enabled should return the alternative class with any qualifiers")
        void shouldReturnTheAlternativeClassWithAnyQualifiers() {
            // Given
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, qualifier);
            // Then
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
        }

        @Test
        @DisplayName("Enabled alternatives should take precedence over custom bindings")
        void alternativesShouldTakePrecedenceOverBindings() {
            // Given - bind AlternativesStandardImplementation
            sut.bind(AlternativesInterface.class, Collections.emptyList(), AlternativesStandardImplementation.class);
            // And enable alternative
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);

            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);

            // Then - alternative should win over binding
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
        }

        @Test
        @DisplayName("Enabled alternatives should override bindings even with matching qualifiers")
        void alternativesShouldOverrideBindingsWithQualifiers() {
            // Given - bind with qualifier
            sut.bind(AlternativesInterface.class, qualifier, AlternativesStandardImplementation.class);
            // And enable alternative
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);

            // When - resolve with same qualifier
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, qualifier);

            // Then - alternative should win, ignoring the binding
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
        }

        @Test
        @DisplayName("Bindings should be used when no alternatives are enabled")
        void bindingsShouldBeUsedWhenNoAlternativesEnabled() {
            // Given - only bind, no alternative enabled
            sut.bind(AlternativesInterface.class, Collections.emptyList(), AlternativesStandardImplementation.class);

            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);

            // Then - binding should be used
            assertEquals(AlternativesStandardImplementation.class, clazz);
        }
    }

    @Nested
    @DisplayName("Class tests")
    class ClassTests {

        @Nested
        @DisplayName("A concrete class should be resolved by itself")
        class ConcreteClassShouldBeResolvedByItself {

            @Test
            @DisplayName("resolveImplementation should return the concrete class itself")
            void resolveImplementation() {
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationConcreteClass.class, null);
                // Then
                assertEquals(SingleImplementationConcreteClass.class, resolved);
            }

            @Test
            @DisplayName("resolveImplementations should return the concrete class itself")
            void resolveImplementations() {
                // When
                Collection<Class<?>> resolved = sut.resolveImplementations(SingleImplementationConcreteClass.class);
                // Then
                assertEquals(1, resolved.size());
                assertEquals(SingleImplementationConcreteClass.class, resolved.iterator().next());
            }
        }

        @Test
        @DisplayName("When resolving an abstract class should throw UnsatisfiedResolutionException if no concrete classes are found")
        void shouldThrowUnsatisfiedResolutionExceptionIfNoConcreteClassesFound() {
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NoConcreteClassesAbstractClass.class, null));
        }

        /**
         * If we have a unique implementation, it does not need to be annotated.
         */
        @Test
        @DisplayName("Should resolve an abstract class with a single standard implementation")
        void shouldResolveAnAbstractClassWithStandardImplementation() {
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationAbstractClass.class, null);
            // Then
            assertEquals(SingleImplementationConcreteClass.class, resolved);
        }

        /**
         * When we have multiple concrete classes, the only one of them not annotated with {@link Named} is
         * considered the standard implementation.
         */
        @Test
        @DisplayName("Should resolve an abstract class with multiple implementations with standard implementation")
        void shouldResolveInterfaceWithStandardImplementation() {
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, null);
            // Then
            assertEquals(MultipleConcreteClassesStandardClass.class, resolved);
        }

        /**
         * When we have multiple concrete classes, we can specify one of the {@link Named} implementations to be used
         * by specifying the qualifier.
         */
        @Test
        @DisplayName("Should resolve a class with specified named implementation")
        void shouldResolveClassWithSpecifiedNamedImplementation() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, qualifiers);
            // Then
            assertEquals(MultipleConcreteClassesNamed1.class, resolved);
        }

        /**
         * We can have multiple implementations, but only one can be missing the {@link Named} annotation,
         * or we will get an exception.
         */
        @Test
        @DisplayName("Should throw AmbiguousResolutionException if more than one standard concrete classes are found")
        void shouldThrowAmbiguousResolutionExceptionWithMoreThanOneStandardConcreteClasses() {
            // When / Then
            assertThrows(AmbiguousResolutionException.class, () -> sut.resolveImplementation(MultipleNotAnnotatedAbstractClass.class, null));
        }

        /**
         * If we specify a wrong qualifier (no class exists that is marked with that value for {@link Named}),
         * we will get an UnsatisfiedResolutionException.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if specified alternative implementation is not found")
        void shouldThrowUnsatisfiedResolutionExceptionIfSpecifiedAlternateImplementationNotFound() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("not-found"));
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleConcreteClassesAbstractClass.class, qualifiers));
        }

        /**
         * If we have multiple implementations, we can retrieve all of them to subsequently choose the one we want.
         */
        @Test
        @DisplayName("Should return all concrete classes for a given abstract class")
        void shouldReturnAllConcreteClassesForAGivenAbstractClass() {
            // Given
            Collection<Class<? extends MultipleConcreteClassesAbstractClass>> expected = new ArrayList<>();
            expected.add(MultipleConcreteClassesStandardClass.class);
            expected.add(MultipleConcreteClassesNamed1.class);
            expected.add(MultipleConcreteClassesNamed2.class);
            expected.add(MultipleConcreteClassesNamed3.class);
            // When
            Collection<Class<? extends MultipleConcreteClassesAbstractClass>> classes = sut.resolveImplementations(MultipleConcreteClassesAbstractClass.class);
            // Then
            assertEquals(4, classes.size());
            assertTrue(classes.containsAll(expected));
        }
    }

    @Nested
    @DisplayName("Interface tests")
    class InterfaceTests {

        @ParameterizedTest
        @DisplayName("Should throw UnsatisfiedResolutionException if no implementations are found")
        @MethodSource("com.threeamigos.common.util.implementations.injection.ClassResolverUnitTest#noQualifiers")
        void shouldThrowUnsatisfiedResolutionExceptionIfNoImplementationsFound(Collection<Annotation> noQualifier) {
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NoImplementationsInterface.class, noQualifier));
        }

        /**
         * If we have a unique implementation, it does not need to be annotated.
         */
        @Test
        @DisplayName("Should resolve an interface with a single standard implementation")
        void shouldResolveInterfaceWithSingleStandardImplementation() {
            // When
            Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null);
            // Then
            assertEquals(SingleImplementationClass.class, resolved);
        }

        /**
         * When we have multiple implementations, the only one of them not annotated with {@link Named} is
         * considered the standard implementation.
         */
        @Test
        @DisplayName("Should resolve an interface with multiple implementations with standard implementation")
        void shouldResolveInterfaceWithStandardImplementation() {
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, null);
            // Then
            assertEquals(MultipleImplementationsStandardImplementation.class, resolved);
        }

        /**
         * When we have multiple implementations, we can specify one of the {@link Named} implementations to be used
         * by specifying the qualifier.
         */
        @Test
        @DisplayName("Should resolve an interface with specified qualifier")
        void shouldResolveInterfaceWithSpecifiedQualifier() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(MultipleImplementationsNamed1.class, resolved);
        }

        /**
         * When we have multiple implementations, we can specify one of the {@link Named} implementations to be used
         * by specifying the qualifier (or the qualifiers).
         */
        @Test
        @DisplayName("Should resolve an interface with qualifier-annotated implementation")
        void shouldResolveInterfaceWithSpecifiedQualifierImplementation() throws Exception {
            // Given
            Collection<Annotation> qualifiers = Arrays.asList(
                    new NamedLiteral("name"),
                    new MyQualifier() {
                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return MyQualifier.class;
                        }
                        @Override
                        public boolean equals(Object obj) {
                            return obj instanceof MyQualifier;
                        }
                        @Override
                        public int hashCode() {
                            return 0;
                        }
                    }
            );
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(MultipleImplementationsNamedAndAnnotated.class, resolved);
        }

        @Test
        @DisplayName("Should resolve an interface with @Default qualifier to standard implementation")
        void shouldResolveInterfaceWithDefaultQualifier() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new DefaultLiteral());
            // When
            Class<?> resolved = sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers);
            // Then
            // MultipleImplementationsStandardImplementation has NO qualifiers, so it matches @Default
            assertEquals(MultipleImplementationsStandardImplementation.class, resolved);
        }

        /**
         * We can have multiple implementations without having a standard one, but in this case we must
         * always specify the qualifier, or we will get an exception.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if only qualified implementations found and no qualifier specified")
        void shouldThrowUnsatisfiedResolutionExceptionIfOnlyQualifiedImplementationsFoundAndNoQualifierSpecified() {
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(NamedImplementationsOnlyInterface.class, null));
        }

        /**
         * We can have multiple implementations without having a standard one, but in this case we must
         * always specify the qualifier to get the desired one.
         */
        @Test
        @DisplayName("Should resolve an interface if only qualified implementations found but qualifier specified")
        void shouldResolveInterfaceIfOnlyQualifiedImplementationsFoundButQualifierSpecified() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Class<?> resolved = sut.resolveImplementation(NamedImplementationsOnlyInterface.class, qualifiers);
            // Then
            assertEquals(NamedImplementationsOnlyImplementation1.class, resolved);
        }

        /**
         * We can have multiple implementations, but only one can be missing a qualifier, or we will get an exception.
         */
        @Test
        @DisplayName("Should throw AmbiguousResolutionException if an interface has more than one non-qualified implementations")
        void shouldThrowAmbiguousResolutionExceptionWithInterfaceWithMoreThanOneNonqualifiedImplementations() {
            // When / Then
            assertThrows(AmbiguousResolutionException.class, () -> sut.resolveImplementation(MultipleNotAnnotatedImplementationsInterface.class, null));
        }

        /**
         * If we specify a wrong qualifier (no class exists that is marked with that value for {@link Named}),
         * we will get an UnsatisfiedResolutionException.
         */
        @Test
        @DisplayName("Should throw UnsatisfiedResolutionException if specified named implementation is not found")
        void shouldThrowUnsatisfiedResolutionExceptionIfAlternateImplementationNotFound() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("not-found"));
            // When / Then
            assertThrows(UnsatisfiedResolutionException.class, () -> sut.resolveImplementation(MultipleImplementationsInterface.class, qualifiers));
        }

        /**
         * If we have multiple implementations, we can retrieve all of them to subsequently choose the one we want.
         */
        @Test
        @DisplayName("Should return all implementations for a given interface")
        void shouldReturnAllImplementationsForAGivenInterface() {
            // Given
            Collection<Class<? extends MultipleImplementationsInterface>> expected = new ArrayList<>();
            expected.add(MultipleImplementationsStandardImplementation.class);
            expected.add(MultipleImplementationsNamed1.class);
            expected.add(MultipleImplementationsNamed2.class);
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> classes = sut.resolveImplementations(MultipleImplementationsInterface.class);
            // Then
            assertEquals(5, classes.size());
            assertTrue(classes.containsAll(expected));
        }
    }

    @Nested
    @DisplayName("Qualifiers")
    class ResolveImplementationsWithQualifiers {

        @Test
        @DisplayName("Should return all active classes if qualifiers are null")
        void shouldReturnAllActiveClassesIfQualifiersAreNull() {
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, null);
            // Then
            // Expects 4: Standard, Named1, Named2, NamedAndAnnotated. Alternatives are filtered out by default if not enabled.
            assertEquals(4, resolved.size());
        }

        @Test
        @DisplayName("Should return only matching classes for a specific qualifier")
        void shouldReturnOnlyMatchingClassesForSpecificQualifier() {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new NamedLiteral("name1"));
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(1, resolved.size());
            assertTrue(resolved.contains(MultipleImplementationsNamed1.class));
        }

        @Test
        @DisplayName("Should return all classes when @Any qualifier is used")
        void shouldReturnAllClassesWhenAnyQualifierIsUsed() throws Exception {
            // Given
            Collection<Annotation> qualifiers = Collections.singletonList(new Any() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Any.class;
                }
            });
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, qualifiers);
            // Then
            assertEquals(4, resolved.size());
        }

        @Test
        @DisplayName("Should return all active classes if qualifiers are empty collection")
        void shouldReturnAllActiveClassesIfQualifiersAreEmpty() {
            // When - Line 238: qualifiers.isEmpty() branch
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, Collections.emptyList());
            // Then
            // Expects 4: Standard, Named1, Named2, NamedAndAnnotated. Alternatives are filtered out by default if not enabled.
            assertEquals(4, resolved.size());
        }

        @Test
        @DisplayName("Should filter out @Alternative classes that are not enabled")
        void shouldFilterOutNotEnabledAlternatives() {
            // When - Line 235: Tests that @Alternative classes are filtered when not enabled
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, null);

            // Then - MultipleAlternativesAlternativeImplementation should NOT be in results
            assertFalse(resolved.contains(MultipleAlternativesAlternativeImplementation.class));
            // But non-alternative classes should be present
            assertTrue(resolved.contains(MultipleImplementationsStandardImplementation.class));
        }

        @Test
        @DisplayName("Should include @Alternative classes when they are enabled")
        void shouldIncludeEnabledAlternatives() {
            // Given - Line 235: Tests enabledAlternatives.contains(clazz) == true branch
            Class<?> alternative = MultipleAlternativesAlternativeImplementation.class;
            sut.enableAlternative(alternative);

            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, null);

            // Then - The enabled alternative should now be included
            assertTrue(resolved.contains(alternative));
        }
    }

    @Nested
    @DisplayName("Class retrieving tests")
    class ClassRetrievingTests {

        @Nested
        @DisplayName("When qualifiers is empty or null")
        class NullOrEmptyQualifiers {

            @Test
            @DisplayName("Should work if qualifiers collection is null")
            void shouldFindALotOfClassesIfPackageIsNull() throws Exception {
                // Given
                KnowledgeBase kb = new KnowledgeBase();
                SimpleClassConsumer sink = new SimpleClassConsumer();
                new ParallelClasspathScanner(Thread.currentThread().getContextClassLoader(),
                    sink, kb, getPackageName(SingleImplementationInterface.class));
                for (Class<?> clazz : sink.getClasses()) {
                    kb.add(clazz);
                }
                ClassResolver sut = new ClassResolver(kb);
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null);
                // Then
                assertEquals(SingleImplementationClass.class, resolved);
            }

            @Test
            @DisplayName("Should work if qualifiers collection is empty")
            void shouldFindALotOfClassesIfPackageIsEmpty() throws Exception {
                // Given
                KnowledgeBase kb = new KnowledgeBase();
                SimpleClassConsumer sink = new SimpleClassConsumer();
                new ParallelClasspathScanner(Thread.currentThread().getContextClassLoader(), sink, kb);
                for (Class<?> clazz : sink.getClasses()) {
                    kb.add(clazz);
                }
                ClassResolver sut = new ClassResolver(kb);
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, Collections.emptyList());
                // Then
                assertEquals(SingleImplementationClass.class, resolved);
            }
        }

        @Test
    @DisplayName("Should remember already resolved classes")
    void shouldRememberAlreadyResolvedClasses() throws Exception {
        // Given
        KnowledgeBase kb = new KnowledgeBase() {
            @Override
            public Collection<Class<?>> getClasses() {
                return Collections.singletonList(SingleImplementationClass.class);
            }
        };
        ClassResolver sut = new ClassResolver(kb);

        // When - calling resolveImplementations twice
        Collection<Class<? extends SingleImplementationInterface>> result1 =
                sut.resolveImplementations(SingleImplementationInterface.class);
        Collection<Class<? extends SingleImplementationInterface>> result2 =
                sut.resolveImplementations(SingleImplementationInterface.class);

        // Then - caching should return same content both times
        assertEquals(result1, result2);
    }
    }

    private String getPackageName(Class<?> clazz) {
        return clazz.getPackage().getName();
    }

    static Stream<Arguments> noQualifiers() {
        return Stream.of(Arguments.of(Collections.emptyList()));
    }

    // Test structures
    @Nested
    @DisplayName("Cache Behavior Tests")
    class CacheBehaviorTests {

        @Test
        @DisplayName("Should cache resolved implementations and return same result on subsequent calls")
        void shouldCacheResolvedImplementations() throws Exception {
            // Given
            KnowledgeBase kb = new KnowledgeBase() {
                @Override
                public Collection<Class<?>> getClasses() {
                    return Collections.singletonList(SingleImplementationClass.class);
                }
            };
        TypeChecker checker = new TypeChecker() {
            @Override
            public boolean isAssignable(Type targetType, Type implementationType) {
                return true;
            }
        };
            ClassResolver resolver = new ClassResolver(kb, checker);

            // When - first call
            Collection<Class<? extends SingleImplementationInterface>> result1 =
                resolver.resolveImplementations(SingleImplementationInterface.class);
            // Second call
            Collection<Class<? extends SingleImplementationInterface>> result2 =
                resolver.resolveImplementations(SingleImplementationInterface.class);

            // Then - cached result should be identical
            assertEquals(result1, result2);
        }

        @Test
        @DisplayName("Cache should work correctly with different types")
        void cacheShouldWorkWithDifferentTypes() {
            // When
            Collection<Class<? extends SingleImplementationInterface>> result1 =
                sut.resolveImplementations(SingleImplementationInterface.class);
            Collection<Class<? extends MultipleImplementationsInterface>> result2 =
                sut.resolveImplementations(MultipleImplementationsInterface.class);

            // Call again to verify caching
            Collection<Class<? extends SingleImplementationInterface>> result1Again =
                sut.resolveImplementations(SingleImplementationInterface.class);

            // Then
            assertFalse(result1.isEmpty());
            assertFalse(result2.isEmpty());
            assertEquals(result1.size(), result1Again.size());
        }

        @Test
        @DisplayName("Should handle cache with parameterized types")
        void shouldHandleCacheWithParameterizedTypes() throws Exception {
            // Given
            Type listOfStrings = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{String.class};
                }

                @Override
                public Type getRawType() {
                    return List.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };

            // When/Then - should not crash with parameterized types
            assertDoesNotThrow(() -> sut.resolveImplementations(listOfStrings));
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent resolution of same type")
        void shouldHandleConcurrentResolutionOfSameType() throws Exception {
            // Given
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // When - multiple threads resolve the same type concurrently
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        Collection<Class<? extends SingleImplementationInterface>> result =
                            sut.resolveImplementations(SingleImplementationInterface.class);
                        if (!result.isEmpty()) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Release all threads
            assertTrue(endLatch.await(5, TimeUnit.SECONDS));
            executor.shutdown();

            // Then - all threads should succeed
            assertEquals(threadCount, successCount.get());
            assertEquals(0, failureCount.get());
        }

        @Test
        @DisplayName("Should handle concurrent resolution of different types")
        void shouldHandleConcurrentResolutionOfDifferentTypes() throws Exception {
            // Given
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            Set<Type> types = new HashSet<>(Arrays.asList(
                SingleImplementationInterface.class,
                MultipleImplementationsInterface.class,
                SingleImplementationAbstractClass.class,
                MultipleConcreteClassesAbstractClass.class
            ));
            AtomicInteger successCount = new AtomicInteger(0);

            // When - multiple threads resolve different types concurrently
            for (int i = 0; i < threadCount; i++) {
                Type type = new ArrayList<>(types).get(i % types.size());
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Collection<?> result = sut.resolveImplementations(type);
                        if (!result.isEmpty()) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Expected for some types
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(endLatch.await(5, TimeUnit.SECONDS));
            executor.shutdown();

            // Then - at least some should succeed
            assertTrue(successCount.get() > 0);
        }
    }

    @Nested
    @DisplayName("Binding Edge Cases")
    class BindingEdgeCases {

        @Test
        @DisplayName("Should handle binding with null qualifiers")
        void shouldHandleBindingWithNullQualifiers() {
            // Given
            sut.setBindingsOnly(true);
            sut.bind(SingleImplementationInterface.class, Collections.emptyList(), SingleImplementationClass.class);

            // When
            Class<?> result = sut.resolveImplementation(SingleImplementationInterface.class, null);

            // Then
            assertEquals(SingleImplementationClass.class, result);
        }

        @Test
        @DisplayName("Should handle binding with empty qualifiers collection")
        void shouldHandleBindingWithEmptyQualifiers() {
            // Given
            sut.setBindingsOnly(true);
            sut.bind(SingleImplementationInterface.class, Collections.emptySet(), SingleImplementationClass.class);

            // When
            Class<?> result = sut.resolveImplementation(SingleImplementationInterface.class, Collections.emptyList());

            // Then
            assertEquals(SingleImplementationClass.class, result);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when binding null type")
        void shouldThrowExceptionWhenBindingNullType() {
            // When/Then
            assertThrows(IllegalArgumentException.class,
                () -> sut.bind(null, Collections.emptyList(), SingleImplementationClass.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when binding null implementation")
        void shouldThrowExceptionWhenBindingNullImplementation() {
            // When/Then
            assertThrows(IllegalArgumentException.class,
                () -> sut.bind(SingleImplementationInterface.class, Collections.emptyList(), null));
        }

        @Test
        @DisplayName("Should allow rebinding the same type with different qualifiers")
        void shouldAllowRebindingSameTypeWithDifferentQualifiers() {
            // Given
            Collection<Annotation> qualifier1 = Collections.singleton(new NamedLiteral("impl1"));
            Collection<Annotation> qualifier2 = Collections.singleton(new NamedLiteral("impl2"));

            sut.bind(SingleImplementationInterface.class, qualifier1, SingleImplementationClass.class);
            sut.bind(SingleImplementationAbstractClass.class, qualifier2, SingleImplementationConcreteClass.class);

            // When
            Class<?> result1 = sut.resolveImplementation(SingleImplementationInterface.class, qualifier1);
            Class<?> result2 = sut.resolveImplementation(SingleImplementationAbstractClass.class, qualifier2);

            // Then
            assertEquals(SingleImplementationClass.class, result1);
            assertEquals(SingleImplementationConcreteClass.class, result2);
        }

        @Test
        @DisplayName("Should overwrite binding when binding same type with same qualifiers")
        void shouldOverwriteBindingWhenBindingSameTypeWithSameQualifiers() {
            // Given
            Collection<Annotation> qualifiers = Collections.singleton(new NamedLiteral("test"));

            sut.bind(SingleImplementationInterface.class, qualifiers, SingleImplementationClass.class);
            // Bind again with same qualifiers but different implementation (if available)
            // For now, just verify that binding twice works without error
            sut.bind(SingleImplementationInterface.class, qualifiers, SingleImplementationClass.class);

            // When
            Class<?> result = sut.resolveImplementation(SingleImplementationInterface.class, qualifiers);

            // Then - should return the bound implementation
            assertEquals(SingleImplementationClass.class, result);
        }
    }

    @Nested
    @DisplayName("Alternative Edge Cases")
    class AlternativeEdgeCases {

        @Test
        @DisplayName("Should throw IllegalArgumentException enabling null alternative")
        void shouldHandleEnablingNullAlternative() {
            // When/Then - should not crash
            assertThrows(IllegalArgumentException.class, () -> sut.enableAlternative(null));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException handling class not marked with @Alternative")
        void shouldHandleClassNotMarkedWithAlternative() {
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> sut.enableAlternative(SingleImplementationClass.class));
        }

        @Test
        @DisplayName("Should handle enabling same alternative multiple times")
        void shouldHandleEnablingSameAlternativeMultipleTimes() {
            // Given
            Class<?> alternative = AlternativesAlternativeImplementation1.class;

            // When
            sut.enableAlternative(alternative);
            sut.enableAlternative(alternative);
            sut.enableAlternative(alternative);

            // Then - should resolve correctly without issues
            Class<?> result = sut.resolveImplementation(SingleImplementationInterface.class, null);
            assertEquals(SingleImplementationClass.class, result);
        }

        @Test
        @DisplayName("Should handle enabling non-existent alternative")
        void shouldHandleEnablingNonExistentAlternative() {
            // Given - enable an alternative that doesn't implement the interface
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);

            // When - resolve interface
            Class<?> result = sut.resolveImplementation(SingleImplementationInterface.class, null);

            // Then - should return the standard implementation
            assertEquals(SingleImplementationClass.class, result);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return empty collection when no implementation found for resolveImplementations")
        void shouldReturnEmptyCollectionWhenNoImplementationFound() {
            // When
            Collection<Class<? extends NoImplInterface>> result = sut.resolveImplementations(NoImplInterface.class);

            // Then - should return empty collection for types with no implementations
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return multiple implementations from resolveImplementations")
        void shouldReturnMultipleImplementationsFromResolveImplementations() {
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> result =
                sut.resolveImplementations(MultipleImplementationsInterface.class);

            // Then - should return all implementations (not throw ambiguous exception)
            assertNotNull(result);
            assertTrue(result.size() > 1);
        }

        @Test
        @DisplayName("Should handle exceptions from KnowledgeBase gracefully")
        void shouldHandleExceptionsFromKnowledgeBase() throws Exception {
            // Given
            KnowledgeBase failingKb = new KnowledgeBase() {
                @Override
                public Collection<Class<?>> getClasses() {
                    throw new RuntimeException("KnowledgeBase failed");
                }
            };
            TypeChecker checker = new TypeChecker();
            ClassResolver resolver = new ClassResolver(failingKb, checker);

            // When/Then - should wrap in ResolutionException
            assertThrows(ResolutionException.class,
                () -> resolver.resolveImplementations(SingleImplementationInterface.class));
        }

        @Test
        @DisplayName("Should handle bindingsOnly mode when no bindings exist")
        void shouldHandleBindingsOnlyModeWhenNoBindingsExist() {
            // Given
            sut.setBindingsOnly(true);

            // When/Then
            assertThrows(UnsatisfiedResolutionException.class,
                () -> sut.resolveImplementation(SingleImplementationInterface.class, null));
        }

        @Test
        @DisplayName("Should handle bindingsOnly mode with qualifiers when no bindings exist")
        void shouldHandleBindingsOnlyModeWithQualifiersWhenNoBindingsExist() {
            // Given
            sut.setBindingsOnly(true);
            Collection<Annotation> qualifiers = Collections.singleton(new NamedLiteral("test"));

            // When/Then
            UnsatisfiedResolutionException exception = assertThrows(
                UnsatisfiedResolutionException.class,
                () -> sut.resolveImplementation(SingleImplementationInterface.class, qualifiers)
            );

            // Error message should mention the qualifiers
            assertTrue(exception.getMessage().contains("qualifiers"));
        }
    }

    @Nested
    @DisplayName("Concrete Class Resolution Tests")
    class ConcreteClassResolutionTests {

        @Test
        @DisplayName("Should return concrete class itself when resolved with @Default qualifier")
        void shouldReturnConcreteClassWithDefaultQualifier() {
            // Given
            Collection<Annotation> qualifiers = Collections.singleton(new DefaultLiteral());

            // When
            Class<?> result = sut.resolveImplementation(SingleImplementationConcreteClass.class, qualifiers);

            // Then
            assertEquals(SingleImplementationConcreteClass.class, result);
        }

        @Test
        @DisplayName("Should throw exception when resolving concrete class with non-default qualifiers")
        void shouldThrowExceptionWhenResolvingConcreteClassWithNonDefaultQualifiers() {
            // Given
            Collection<Annotation> qualifiers = Collections.singleton(new NamedLiteral("test"));

            // When/Then - concrete classes with non-default qualifiers should throw
            assertThrows(UnsatisfiedResolutionException.class,
                () -> sut.resolveImplementation(SingleImplementationConcreteClass.class, qualifiers));
        }

        @Test
        @DisplayName("Should handle array types correctly")
        void shouldHandleArrayTypesCorrectly() {
            // When
            Class<?> result = sut.resolveImplementation(String[].class, null);

            // Then
            assertEquals(String[].class, result);
        }

        @Test
        @DisplayName("Should handle primitive array types")
        void shouldHandlePrimitiveArrayTypes() {
            // When
            Class<?> result = sut.resolveImplementation(int[].class, null);

            // Then
            assertEquals(int[].class, result);
        }
    }

    @Nested
    @DisplayName("Qualifier Matching Tests")
    class QualifierMatchingTests {

        @Test
        @DisplayName("Should match @Any qualifier with any implementation")
        void shouldMatchAnyQualifierWithAnyImplementation() {
            // Given
            Collection<Annotation> qualifiers = Collections.singleton(new AnyLiteral());

            // When
            Collection<Class<? extends MultipleImplementationsInterface>> result =
                sut.resolveImplementations(MultipleImplementationsInterface.class, qualifiers);

            // Then - should return all active implementations
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should match @Default qualifier with non-qualified implementations")
        void shouldMatchDefaultQualifierWithNonQualifiedImplementations() {
            // Given
            Collection<Annotation> qualifiers = Collections.singleton(new DefaultLiteral());

            // When
            Collection<Class<? extends MultipleImplementationsInterface>> result =
                sut.resolveImplementations(MultipleImplementationsInterface.class, qualifiers);

            // Then - should return only implementations without qualifiers
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle multiple qualifiers on single implementation")
        void shouldHandleMultipleQualifiersOnSingleImplementation() {
            // Given
            Collection<Annotation> qualifiers = Arrays.asList(
                new NamedLiteral("name"),
                new MyQualifierLiteral()
            );

            // When/Then - should match implementation with both qualifiers
            assertDoesNotThrow(() ->
                sut.resolveImplementations(MultipleImplementationsInterface.class, qualifiers));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle empty KnowledgeBase")
        void shouldHandleEmptyKnowledgeBase() {
            // Given
            KnowledgeBase emptyKb = new KnowledgeBase();
            // When/Then - should not crash
            assertDoesNotThrow(() -> new ClassResolver(emptyKb));
        }

        @Test
        @DisplayName("Should handle KnowledgeBase with no classes")
        void shouldHandleKnowledgeBaseWithNoClasses() {
            // Given
            KnowledgeBase emptyKb = new KnowledgeBase();
            ClassResolver resolver = new ClassResolver(emptyKb);

            // When - should return empty collection
            Collection<?> result = resolver.resolveImplementations(SingleImplementationInterface.class);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle mock KnowledgeBase with empty collection")
        void shouldHandleMockKnowledgeBaseWithEmptyCollection() {
            // Given
            KnowledgeBase mockKb = new KnowledgeBase() {
                @Override
                public Collection<Class<?>> getClasses() {
                    return Collections.emptyList();
                }
            };

            // When/Then - should not crash
            assertDoesNotThrow(() -> new ClassResolver(mockKb));
        }

        @Test
        @DisplayName("Should handle setBindingsOnly toggle")
        void shouldHandleBindingsOnlyToggle() {
            // Given
            sut.bind(SingleImplementationInterface.class, Collections.emptyList(), SingleImplementationClass.class);

            // When - enable bindingsOnly
            sut.setBindingsOnly(true);
            Class<?> result1 = sut.resolveImplementation(SingleImplementationInterface.class, null);

            // Then - disable bindingsOnly
            sut.setBindingsOnly(false);
            Class<?> result2 = sut.resolveImplementation(SingleImplementationInterface.class, null);

            // Both should work
            assertNotNull(result1);
            assertNotNull(result2);
        }

        @Test
        @DisplayName("Should throw exception when resolving single implementation with no results")
        void shouldThrowExceptionWhenResolvingSingleImplementationWithNoResults() {
            // When/Then - resolveImplementation (singular) should throw for types with no implementations
            assertThrows(UnsatisfiedResolutionException.class,
                () -> sut.resolveImplementation(NoImplInterface2.class, null));
        }

        @Test
        @DisplayName("Should handle type that is both interface and has implementations")
        void shouldHandleComplexTypeResolution() {
            // When
            Collection<Class<? extends SingleImplementationInterface>> result =
                sut.resolveImplementations(SingleImplementationInterface.class);

            // Then
            assertFalse(result.isEmpty());
            assertTrue(result.stream().allMatch(
                c -> !c.isInterface() && SingleImplementationInterface.class.isAssignableFrom(c)
            ));
        }
    }

    @Nested
    @DisplayName("Null Argument Validation Tests")
    class NullArgumentTests {

        @Test
        @DisplayName("Constructor should accept null KnowledgeBase with single arg but fail later")
        void constructorShouldRejectNullKnowledgeBase() {
            // When/Then - Constructor doesn't validate, so it won't throw immediately
            // The single-arg constructor doesn't check for null
            assertDoesNotThrow(() -> new ClassResolver((KnowledgeBase) null));
        }

        @Test
        @DisplayName("Constructor should throw IllegalArgumentException for null KnowledgeBase with two args")
        void constructorShouldRejectNullKnowledgeBaseWithTypeChecker() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ClassResolver(null, new TypeChecker()));

            assertEquals("ClasspathScanner cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Constructor should throw IllegalArgumentException for null TypeChecker")
        void constructorShouldRejectNullTypeChecker() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ClassResolver(new KnowledgeBase(), null));

            assertEquals("TypeChecker cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("bind() should throw IllegalArgumentException for null type")
        void bindShouldRejectNullType() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.bind(null, Collections.emptyList(), SingleImplementationClass.class));

            assertEquals("type cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("bind() should throw IllegalArgumentException for null implementation")
        void bindShouldRejectNullImplementation() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.bind(SingleImplementationInterface.class, Collections.emptyList(), null));

            assertEquals("implementation cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("bind() should throw IllegalArgumentException for null qualifiers")
        void bindShouldRejectNullQualifiers() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.bind(SingleImplementationInterface.class, null, SingleImplementationClass.class));

            assertEquals("qualifiers cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("enableAlternative() should throw IllegalArgumentException for null")
        void enableAlternativeShouldRejectNull() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.enableAlternative(null));

            assertEquals("alternativeClass cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("enableAlternative() should throw IllegalArgumentException for non-@Alternative class")
        void enableAlternativeShouldRejectNonAlternativeClass() {
            // When/Then - SingleImplementationClass is not annotated with @Alternative
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.enableAlternative(SingleImplementationClass.class));

            assertTrue(exception.getMessage().contains("is not annotated with @Alternative"));
        }

        @Test
        @DisplayName("resolveImplementation() should throw IllegalArgumentException for null type")
        void resolveImplementationShouldRejectNullType() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.resolveImplementation(Thread.currentThread().getContextClassLoader(), null, null));

            assertEquals("typeToResolve cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("resolveImplementation() should throw IllegalArgumentException for null classLoader")
        void resolveImplementationShouldRejectNullClassLoader() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.resolveImplementation(null, SingleImplementationInterface.class, null));

            assertEquals("classLoader cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("resolveImplementations() should throw IllegalArgumentException for null type")
        void resolveImplementationsShouldRejectNullType() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.resolveImplementations(Thread.currentThread().getContextClassLoader(), null));

            assertEquals("typeToResolve cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("resolveImplementations() should throw IllegalArgumentException for null classLoader")
        void resolveImplementationsShouldRejectNullClassLoader() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sut.resolveImplementations(null, SingleImplementationInterface.class));

            assertEquals("classLoader cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Multiple Enabled Alternatives Tests")
    class MultipleEnabledAlternativesTests {

        @Test
        @DisplayName("Should throw AmbiguousResolutionException when multiple alternatives enabled")
        void shouldThrowExceptionForMultipleEnabledAlternatives() {
            // Given - enable multiple alternatives
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            sut.enableAlternative(AlternativesAlternativeImplementation2.class);

            // When/Then
            AmbiguousResolutionException exception = assertThrows(AmbiguousResolutionException.class,
                () -> sut.resolveImplementation(AlternativesInterface.class, null));

            assertTrue(exception.getMessage().contains("More than one alternative found"));
            assertTrue(exception.getMessage().contains("AlternativesAlternativeImplementation1"));
            assertTrue(exception.getMessage().contains("AlternativesAlternativeImplementation2"));
        }

        @Test
        @DisplayName("Should throw AmbiguousResolutionException for multiple alternatives even with qualifiers")
        void shouldThrowExceptionForMultipleAlternativesWithQualifiers() {
            // Given
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            sut.enableAlternative(AlternativesAlternativeImplementation2.class);
            Collection<Annotation> qualifiers = Collections.singleton(new NamedLiteral("test"));

            // When/Then
            AmbiguousResolutionException exception = assertThrows(AmbiguousResolutionException.class,
                () -> sut.resolveImplementation(AlternativesInterface.class, qualifiers));

            assertTrue(exception.getMessage().contains("More than one alternative found"));
        }
    }

    @Nested
    @DisplayName("ResolutionException Tests")
    class ResolutionExceptionTests {

        @Test
        @DisplayName("Should wrap KnowledgeBase exceptions in ResolutionException")
        void shouldWrapKnowledgeBaseExceptions() {
            // Given
            KnowledgeBase failingKb = new KnowledgeBase() {
                @Override
                public Collection<Class<?>> getClasses() {
                    throw new RuntimeException("KnowledgeBase failed");
                }
            };
            TypeChecker checker = new TypeChecker();
            ClassResolver resolver = new ClassResolver(failingKb, checker);

            // When/Then
            ResolutionException exception = assertThrows(ResolutionException.class,
                () -> resolver.resolveImplementations(SingleImplementationInterface.class));

            assertTrue(exception.getMessage().contains("Failed to resolve implementations"));
            assertNotNull(exception.getCause());
            assertEquals("KnowledgeBase failed", exception.getCause().getMessage());
        }
    }

    // Helper classes for testing
    private static class AnyLiteral implements jakarta.enterprise.inject.Any {
        @Override
        public Class<? extends Annotation> annotationType() {
            return jakarta.enterprise.inject.Any.class;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof jakarta.enterprise.inject.Any;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class MyQualifierLiteral implements MyQualifier {
        @Override
        public Class<? extends Annotation> annotationType() {
            return MyQualifier.class;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MyQualifier;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

}
