package com.threeamigos.common.util.implementations.injection;

import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed1;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesNamed2;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.MultipleConcreteClassesStandardClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multipleconcreteclasses.subpackage.MultipleConcreteClassesNamed3;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationAbstractClass;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesAlternativeImplementation1;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesInterface;
import com.threeamigos.common.util.implementations.injection.alternatives.AlternativesStandardImplementation;
import com.threeamigos.common.util.implementations.injection.bind.*;
import com.threeamigos.common.util.implementations.injection.interfaces.multipleimplementations.*;
import com.threeamigos.common.util.implementations.injection.interfaces.namedimplementationsonly.NamedImplementationsOnlyImplementation1;
import com.threeamigos.common.util.implementations.injection.interfaces.namedimplementationsonly.NamedImplementationsOnlyInterface;
import com.threeamigos.common.util.implementations.injection.abstractclasses.singleimplementation.SingleImplementationConcreteClass;
import com.threeamigos.common.util.implementations.injection.abstractclasses.noconcreteclasses.NoConcreteClassesAbstractClass;
import com.threeamigos.common.util.implementations.injection.interfaces.noimplementations.NoImplementationsInterface;
import com.threeamigos.common.util.implementations.injection.abstractclasses.multiplenotannotatedconcreteclasses.MultipleNotAnnotatedAbstractClass;
import com.threeamigos.common.util.implementations.injection.interfaces.singleimplementation.SingleImplementationClass;
import com.threeamigos.common.util.implementations.injection.interfaces.singleimplementation.SingleImplementationInterface;
import com.threeamigos.common.util.implementations.injection.interfaces.multiplenotannotatedimplementations.MultipleNotAnnotatedImplementationsInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ClassResolver unit tests")
class ClassResolverUnitTest {

    ClassResolver sut;

    Collection<Annotation> qualifier = Collections.singleton(AnnotationLiteral.of(BindingQualifier.class));
    Collection<Annotation> nonMatchingQualifier = Collections.singleton(AnnotationLiteral.of(BindingNotMatchingQualifier.class));

    @BeforeEach
    void setUp() {
        // Given
        sut = new ClassResolver(getPackageName(ClassResolverUnitTest.class));
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
            void classWithNoQualifierShouldResolveWithNoQualifier(Collection<Annotation> noQualifier) throws Exception {
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
            void classWithQualifierShouldResolveWithQualifier() throws Exception {
                //Given
                sut.bind(ClassToBind.class, qualifier, TargetClass.class);
                // When
                Class<?> clazz = sut.resolveImplementation(ClassToBind.class, qualifier);
                assertEquals(TargetClass.class, clazz);
            }

            @Test
            @DisplayName("A class bound with qualifier should not be resolved when resolving with non-matching qualifiers")
            void classWithQualifierShouldResolveWithNonMatchingQualifier() throws Exception {
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
            void interfaceWithNoQualifierShouldResolveWithNoQualifier(Collection<Annotation> noQualifier) throws Exception {
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
            void interfaceWithQualifierShouldResolveWithQualifier() throws Exception {
                //Given
                sut.bind(InterfaceToBind.class, qualifier, TargetImplementation.class);
                // When
                Class<?> clazz = sut.resolveImplementation(InterfaceToBind.class, qualifier);
                assertEquals(TargetImplementation.class, clazz);
            }

            @Test
            @DisplayName("An interface bound with qualifier should not be resolved when resolving with non-matching qualifiers")
            void classWithQualifierShouldResolveWithNonMatchingQualifier() throws Exception {
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
        void shouldReturnTheDefaultClass() throws Exception{
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);
            // Then
            assertEquals(AlternativesStandardImplementation.class, clazz);
        }

        @Test
        @DisplayName("If alternatives are enabled should return the alternative class with no qualifiers")
        void shouldReturnTheAlternativeClassWithNoQualifiers() throws Exception{
            // Given
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, null);
            // Then
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
        }

        @Test
        @DisplayName("If alternatives are enabled should return the alternative class with any qualifiers")
        void shouldReturnTheAlternativeClassWithAnyQualifiers() throws Exception{
            // Given
            sut.enableAlternative(AlternativesAlternativeImplementation1.class);
            // When
            Class<?> clazz = sut.resolveImplementation(AlternativesInterface.class, qualifier);
            // Then
            assertEquals(AlternativesAlternativeImplementation1.class, clazz);
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
            void resolveImplementation() throws Exception {
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationConcreteClass.class, null);
                // Then
                assertEquals(SingleImplementationConcreteClass.class, resolved);
            }

            @Test
            @DisplayName("resolveImplementations should return the concrete class itself")
            void resolveImplementations() throws Exception {
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
        void shouldResolveAnAbstractClassWithStandardImplementation() throws Exception {
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
        void shouldResolveInterfaceWithStandardImplementation() throws Exception {
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
        void shouldResolveClassWithSpecifiedNamedImplementation() throws Exception {
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
        void shouldReturnAllConcreteClassesForAGivenAbstractClass() throws Exception {
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
        void shouldResolveInterfaceWithSingleStandardImplementation() throws Exception {
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
        void shouldResolveInterfaceWithStandardImplementation() throws Exception {
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
        void shouldResolveInterfaceWithSpecifiedQualifier() throws Exception {
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
        void shouldResolveInterfaceWithDefaultQualifier() throws Exception {
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
        void shouldResolveInterfaceIfOnlyQualifiedImplementationsFoundButQualifierSpecified() throws Exception {
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
        void shouldReturnAllImplementationsForAGivenInterface() throws Exception {
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
        void shouldReturnAllActiveClassesIfQualifiersAreNull() throws Exception {
            // When
            Collection<Class<? extends MultipleImplementationsInterface>> resolved = sut.resolveImplementations(
                    MultipleImplementationsInterface.class, null);
            // Then
            // Expects 4: Standard, Named1, Named2, NamedAndAnnotated. Alternatives are filtered out by default if not enabled.
            assertEquals(4, resolved.size());
        }

        @Test
        @DisplayName("Should return only matching classes for a specific qualifier")
        void shouldReturnOnlyMatchingClassesForSpecificQualifier() throws Exception {
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
                ClassResolver sut = new ClassResolver(getPackageName(SingleImplementationInterface.class));
                // When
                Class<?> resolved = sut.resolveImplementation(SingleImplementationInterface.class, null);
                // Then
                assertEquals(SingleImplementationClass.class, resolved);
            }

            @Test
            @DisplayName("Should work if qualifiers collection is empty")
            void shouldFindALotOfClassesIfPackageIsEmpty() throws Exception {
                // Given
                ClassResolver sut = new ClassResolver();
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
            String packageName = "com.threeamigos";
            ClassResolver sut = new ClassResolver(packageName);
            ClassLoader mockLoader = mock(ClassLoader.class);
            String expectedPath = "com/threeamigos";

            // Stub getResources to return an empty enumeration so the loop finishes
            when(mockLoader.getResources(expectedPath)).thenReturn(Collections.emptyEnumeration());

            // When - calling the public method that internally calls getClasses
            sut.resolveImplementations(mockLoader, SingleImplementationInterface.class);
            sut.resolveImplementations(mockLoader, SingleImplementationInterface.class);

            // Then - Verify the ClassLoader was queried exactly once
            verify(mockLoader, times(1)).getResources(expectedPath);
        }
    }

    private String getPackageName(Class<?> clazz) {
        return clazz.getPackage().getName();
    }

    static Stream<Arguments> noQualifiers() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(Collections.emptyList()));
    }

    // Test structures
    interface MyGeneric<T> {}

}